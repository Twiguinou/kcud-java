package showoff.Vulkan;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import showoff.Disposable;
import showoff.FrameAllocator;

import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySession;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK13.*;
import static org.lwjgl.util.shaderc.Shaderc.*;

public class ShaderModule implements Disposable
{
    public record CompilingDescription(String filename, boolean optimize) {}

    private final long m_internalHandle;
    private final int m_stage;
    private final VkDevice device;
    private final ByteBuffer m_entryPoint;

    private static int mapShaderStage(int stage) throws VulkanException
    {
        return switch (stage) {
            case VK_SHADER_STAGE_VERTEX_BIT -> shaderc_vertex_shader;
            case VK_SHADER_STAGE_FRAGMENT_BIT -> shaderc_fragment_shader;
            case VK_SHADER_STAGE_GEOMETRY_BIT -> shaderc_geometry_shader;
            case VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT -> shaderc_tess_control_shader;
            case VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT -> shaderc_tess_evaluation_shader;
            case VK_SHADER_STAGE_COMPUTE_BIT -> shaderc_compute_shader;
            default -> throw new VulkanException("Unsupported shader stage: " + stage);
        };
    }

    public ShaderModule(VkDevice device, int stage, InputStream istream, String entryPoint, @Nullable CompilingDescription compilation) throws VulkanException, IOException
    {
        this.device = device;
        this.m_stage = stage;
        try (MemorySession session = MemorySession.openConfined(); FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            ByteBuffer data;
            final byte[] bytes = istream.readAllBytes();
            data = session.allocate(bytes.length).asByteBuffer().put(0, bytes);
            if (compilation != null)
            {
                long compiler = shaderc_compiler_initialize();
                long options = shaderc_compile_options_initialize();
                if (compiler == MemoryUtil.NULL || options == MemoryUtil.NULL)
                {
                    throw new VulkanException("Failed to create shaderc context.");
                }
                final int shaderc_stage = mapShaderStage(this.m_stage);
                if (shaderc_stage == shaderc_vertex_shader) shaderc_compile_options_set_invert_y(options, true);
                if (compilation.optimize) shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_performance);

                long result = shaderc_compile_into_spv(compiler, data, shaderc_stage, allocator.UTF8(compilation.filename), allocator.UTF8(entryPoint), options);
                shaderc_compile_options_release(options);
                shaderc_compiler_release(compiler);
                if (result == MemoryUtil.NULL)
                {
                    throw new VulkanException("Failed to compile shader: " + compilation.filename);
                }
                if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success)
                {
                    throw new VulkanException(String.format("Failed to compile shader{%s} -> %s", compilation.filename, nshaderc_result_get_error_message(result)));
                }

                ByteBuffer shaderc_data = shaderc_result_get_bytes(result);
                data = session.allocate(shaderc_data.remaining()).asByteBuffer().put(shaderc_data);
                data.flip();
                shaderc_result_release(result);
            }

            VkShaderModuleCreateInfo shaderModuleCreateInfo = VkShaderModuleCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(data);
            LongBuffer pShaderModule = allocator.mallocLong(1);
            VulkanException.check(vkCreateShaderModule(this.device, shaderModuleCreateInfo, null, pShaderModule), "Failed to create shader module.");
            this.m_internalHandle = pShaderModule.get();
            this.m_entryPoint = MemoryUtil.memUTF8(entryPoint);
        }
    }

    public long get()
    {
        return this.m_internalHandle;
    }

    public int getStage()
    {
        return this.m_stage;
    }

    public ByteBuffer getEntryPoint()
    {
        return this.m_entryPoint;
    }

    @Override
    public void dispose()
    {
        vkDestroyShaderModule(this.device, this.m_internalHandle, null);
        MemoryUtil.memFree(this.m_entryPoint);
    }
}
