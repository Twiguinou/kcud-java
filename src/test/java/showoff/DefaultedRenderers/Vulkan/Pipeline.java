package showoff.DefaultedRenderers.Vulkan;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import showoff.Disposable;
import showoff.FrameAllocator;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySession;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.vulkan.VK13.*;

public abstract class Pipeline implements Disposable
{
    protected final VkDevice device;

    protected Pipeline(VkDevice device)
    {
        this.device = device;
    }

    public abstract long get();
    public abstract long getLayout();

    protected long createShaderModule(String filename, ByteBuffer entryPoint, int kind, boolean compile) throws VulkanException
    {
        try (MemorySession bloat_session = MemorySession.openConfined(); FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            ByteBuffer data;
            try (InputStream istream = Thread.currentThread().getContextClassLoader().getResourceAsStream("shaders/" + filename))
            {
                byte[] bytes = istream.readAllBytes();
                data = bloat_session.allocate(bytes.length).asByteBuffer().put(0, bytes);
                if (compile)
                {
                    final long compiler = shaderc_compiler_initialize();
                    final long options = shaderc_compile_options_initialize();
                    if (compiler == MemoryUtil.NULL || options == MemoryUtil.NULL) throw new VulkanException("Failed to create shaderc context");
                    if (kind == shaderc_vertex_shader) shaderc_compile_options_set_invert_y(options, true);
                    shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_performance);
                    final long result = shaderc_compile_into_spv(compiler, data, kind, allocator.UTF8(filename), entryPoint, options);
                    if (result == MemoryUtil.NULL) throw new VulkanException("Failed to compile shader: " + filename);
                    if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) throw new VulkanException(String.format("Failed to compile shader{%s} -> %s", filename, shaderc_result_get_error_message(result)));
                    shaderc_compile_options_release(options);
                    shaderc_compiler_release(compiler);
                    ByteBuffer shaderc_data = shaderc_result_get_bytes(result);
                    data = bloat_session.allocate(shaderc_data.capacity()).asByteBuffer().put(shaderc_data);
                    data.flip();
                    shaderc_result_release(result);
                }
            }
            catch (IOException e)
            {
                throw new VulkanException(e.toString());
            }
            VkShaderModuleCreateInfo shaderModuleCreateInfo = VkShaderModuleCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(data);
            LongBuffer pShaderModule = allocator.mallocLong(1);
            VulkanException.check(vkCreateShaderModule(this.device, shaderModuleCreateInfo, null, pShaderModule));
            return pShaderModule.get(0);
        }
    }

    @Override
    public void dispose()
    {
        vkDestroyPipeline(this.device, this.get(), null);
        vkDestroyPipelineLayout(this.device, this.getLayout(), null);
    }
}
