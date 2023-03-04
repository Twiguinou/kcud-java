package showoff.App.Render.scene;

import kcud.ContraptionNalgebra.kdMatrix4;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;
import showoff.App.Render.Camera;
import showoff.Either;
import showoff.FrameAllocator;
import showoff.Vulkan.CommandPool;
import showoff.Vulkan.DescriptorPool;
import showoff.Vulkan.DescriptorSets;
import showoff.Vulkan.GraphicsPipeline;
import showoff.Vulkan.LogicalDevice;
import showoff.Vulkan.ShaderModule;
import showoff.Vulkan.VulkanBuffer;
import showoff.Vulkan.VulkanHelpers;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.FloatBuffer;
import java.util.Arrays;

import static org.lwjgl.vulkan.VK13.*;

public class StaticGridRenderer implements SceneObjectRenderer
{
    private final ShaderModule m_vertexShader, m_fragmentShader;
    private final GraphicsPipeline m_pipeline;
    private final DescriptorPool m_descriptorPool;
    private final DescriptorSets m_descriptorSets;
    private final DescriptorSets.Layout m_descriptorSetLayout;

    private final VulkanBuffer m_vertexBuffer, m_indexBuffer;
    private final VulkanBuffer m_uniformBuffer;

    public StaticGridRenderer(LogicalDevice logicalDevice, long renderPass, CommandPool uploadCommandPool, int graphicsQueueFamily, LogicalDevice.Queue transferQueue, int sampleCount, int frameCount)
    {
        this.m_vertexShader = VulkanHelpers.loadShaderModule(logicalDevice.get(), "grid-vs.glsl", VK_SHADER_STAGE_VERTEX_BIT);
        this.m_fragmentShader = VulkanHelpers.loadShaderModule(logicalDevice.get(), "grid-fs.glsl", VK_SHADER_STAGE_FRAGMENT_BIT);

        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            MemorySegment vertices = allocator.allocateArray(ValueLayout.JAVA_FLOAT,
                    -1.f, -1.f,
                    1.f, -1.f,
                    1.f, 1.f,
                    -1.f, 1.f);
            this.m_vertexBuffer = logicalDevice.getAllocationHelper().createBuffer(allocator, vertices.byteSize(), VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT, new int[]{graphicsQueueFamily}, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
            this.m_vertexBuffer.upload(allocator, uploadCommandPool, transferQueue, logicalDevice.getAllocationHelper(), vertices.address().toRawLongValue(), vertices.byteSize());
            MemorySegment indices = allocator.allocateArray(ValueLayout.JAVA_INT,
                    0, 1, 2,
                    0, 2, 3,
                    2, 1, 0,
                    3, 2, 0);
            this.m_indexBuffer = logicalDevice.getAllocationHelper().createBuffer(allocator, indices.byteSize(), VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT, new int[]{graphicsQueueFamily}, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
            this.m_indexBuffer.upload(allocator, uploadCommandPool, transferQueue, logicalDevice.getAllocationHelper(), indices.address().toRawLongValue(), indices.byteSize());

            this.m_uniformBuffer = logicalDevice.getAllocationHelper().createBuffer(allocator, 3 * Float.BYTES, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, new int[]{graphicsQueueFamily}, VK_MEMORY_PROPERTY_HOST_COHERENT_BIT | VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
        }

        this.m_descriptorPool = new DescriptorPool(logicalDevice.get(), new DescriptorPool.DescriptorPoolSize[] {
                new DescriptorPool.DescriptorPoolSize(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, frameCount)
        }, frameCount, true);
        this.m_descriptorSetLayout = new DescriptorSets.Layout(logicalDevice.get(), new DescriptorSets.Layout.Uniform[] {
                new DescriptorSets.Layout.Uniform(0, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 1, VK_SHADER_STAGE_FRAGMENT_BIT)
        });
        long[] layouts = new long[frameCount];
        Arrays.fill(layouts, this.m_descriptorSetLayout.get());
        this.m_descriptorSets = new DescriptorSets(logicalDevice.get(), this.m_descriptorPool, layouts);

        this.m_pipeline = new GraphicsPipeline(logicalDevice.get(), renderPass, new GraphicsPipeline.Description(
                new GraphicsPipeline.VertexShaderStage(this.m_vertexShader, new GraphicsPipeline.VertexShaderStage.InputData[] {
                        new GraphicsPipeline.VertexShaderStage.InputData(2 * Float.BYTES, true)
                }, new GraphicsPipeline.VertexShaderStage.Attribute[] {
                        new GraphicsPipeline.VertexShaderStage.Attribute(0, VK_FORMAT_R32G32_SFLOAT, 0)
                }), this.m_fragmentShader,
                null, null, null, new GraphicsPipeline.PushConstants[] {
                        new GraphicsPipeline.PushConstants(VK_SHADER_STAGE_VERTEX_BIT, 0, 32 * Float.BYTES)
                }, new long[]{this.m_descriptorSetLayout.get()},
                Either.ofRight(1), Either.ofRight(1), sampleCount
        ));
    }

    @Override
    public void render(MemoryStack renderStack, VkCommandBuffer commandBuffer, VkViewport.Buffer viewports, VkRect2D.Buffer scissors, Camera camera, int frameIndex)
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            vkCmdBindVertexBuffers(commandBuffer, 0, allocator.longs(this.m_vertexBuffer.get()), allocator.longs(0L));
            vkCmdBindIndexBuffer(commandBuffer, this.m_indexBuffer.get(), 0L, VK_INDEX_TYPE_UINT32);
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, this.m_pipeline.get());
            vkCmdSetViewport(commandBuffer, 0, viewports);
            vkCmdSetScissor(commandBuffer, 0, scissors);

            FloatBuffer constantsBuffer = allocator.mallocFloat(32);
            camera.getProjection(constantsBuffer);
            camera.getModelView(constantsBuffer.slice(16, 16), new kdMatrix4().identity());
            vkCmdPushConstants(commandBuffer, this.m_pipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, constantsBuffer);

            constantsBuffer.limit(3);
            constantsBuffer.put(0, new float[]{1.f, 0.8f, 0.2f});
            this.m_uniformBuffer.put(allocator, MemoryUtil.memAddress(constantsBuffer), 3 * Float.BYTES);
            this.m_descriptorSets.updateUniformBuffer(this.m_descriptorSets.get()[frameIndex], this.m_uniformBuffer.get(), 3 * Float.BYTES, 0);
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, this.m_pipeline.getLayout(), 0, allocator.longs(this.m_descriptorSets.get()[frameIndex]), null);

            vkCmdDrawIndexed(commandBuffer, 12, 1, 0, 0, 0);
        }
    }

    @Override
    public void destroy()
    {
        this.m_pipeline.dispose();
        this.m_descriptorSets.dispose();
        this.m_descriptorSetLayout.dispose();
        this.m_descriptorPool.dispose();

        this.m_indexBuffer.free();
        this.m_vertexBuffer.free();
        this.m_vertexShader.dispose();
        this.m_fragmentShader.dispose();
    }
}
