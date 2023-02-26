package showoff.App.Render;

import kcud.ContraptionNalgebra.kdMatrix4;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import showoff.App.Feature.NativeOBJLoader;
import showoff.App.Feature.OBJModel;
import showoff.App.Feature.OBJModelLoader;
import showoff.Vulkan.*;
import showoff.Vulkan.ext.Swapchain;
import showoff.Vulkan.ext.VulkanRenderContext;
import showoff.Either;
import showoff.FrameAllocator;
import showoff.WindowContext.WindowProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.lwjgl.vulkan.VK13.*;

public class VulkanRenderer
{
    private static final Logger gRendererLogger = LogManager.getLogger("Vulkan Renderer");
    private static final int gFrameCount = 2;
    private static final int g_MSAA_sampleCount = VK_SAMPLE_COUNT_4_BIT;

    private final VulkanRenderContext m_context;
    private int m_currentFrame = 0;
    private final LogicalDevice m_logicalDevice;
    private final LogicalDevice.Queue m_graphicsQueue, m_presentQueue, m_transferQueue;
    private final Swapchain m_swapchain;
    private final CommandPool m_commandPool;
    private final VulkanImage m_colorImage, m_depthImage;
    private final RenderPass m_renderPass;
    private final GraphicsPipeline m_scenePipeline, m_gridPipeline;
    private final VulkanSync m_syncObjects;
    private final VkCommandBuffer[] m_commandBuffers;
    private final CommandPool m_uploadCommandPool;

    //private final DescriptorPool m_gridDescriptorPool;
    //private final DescriptorSet m_gridDescriptorSet;
    //private final VulkanBuffer m_gridUniformBuffer;
    private final ShaderModule[] m_planarShaders = new ShaderModule[2], m_gridShaders = new ShaderModule[2];
    private final int m_bunnyIndexCount;
    private final VulkanBuffer m_vertexBuffer, m_gridVertexBuffer;
    private final VulkanBuffer m_indexBuffer, m_gridIndexBuffer;

    private static ShaderModule loadShaderModule(VkDevice device, String filepath, int stage) throws VulkanException
    {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("shaders/" + filepath))
        {
            if (stream == null) throw new VulkanException("Failed to load resource: " + filepath);
            return new ShaderModule(device, stage, stream, "main", new ShaderModule.CompilingDescription(filepath, true));
        }
        catch (IOException e)
        {
            throw new VulkanException(e.toString());
        }
    }

    public VulkanRenderer(String name, WindowProcessor windowProc, boolean debug)
    {
        this.m_context = new VulkanRenderContext(name, 0, "kcud", 0, VK_API_VERSION_1_3, windowProc.getVulkanExtensions(),
                debug ? VulkanRenderContext::DefaultCallbackFunction : null, windowProc);
        this.m_context.findSuitableDevice((first, second) ->
        {
            int i = 0;
            if (first.isDedicated()) ++i;
            if (second.isDedicated()) --i;
            if (first.features().samplerAnisotropy()) ++i;
            if (second.features().samplerAnisotropy()) --i;
            if (VulkanHelpers.getMaxUsableSampleCount(first.properties()) > VulkanHelpers.getMaxUsableSampleCount(second.properties())) ++i;
            return i > 0 ? first : second;
        });
        gRendererLogger.info("Selected physical device: " + this.m_context.getPhysicalDevice().properties().deviceNameString());

        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            LogicalDevice.QueueRequirements[] queueRequirements = new LogicalDevice.QueueRequirements[] {
                    new LogicalDevice.QueueRequirements(f -> (f & VK_QUEUE_GRAPHICS_BIT) != 0, new long[]{},
                            new LogicalDevice.QueueRequirements(f -> true, new long[]{this.m_context.getSurface()}, null, false),
                            false),
                    new LogicalDevice.QueueRequirements(f -> (f & VK_QUEUE_TRANSFER_BIT) != 0, new long[]{}, null, true)
            };

            VkPhysicalDeviceFeatures2 features2 = VkPhysicalDeviceFeatures2.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
                    .features(f -> f
                            .samplerAnisotropy(true)
                            .sampleRateShading(true));
            VkPhysicalDeviceVulkan12Features features = VkPhysicalDeviceVulkan12Features.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)
                    .pNext(features2.address())
                    .timelineSemaphore(true);
            List<String> extensions = new LinkedList<>();
            extensions.add(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME);
            this.m_logicalDevice = new LogicalDevice(this.m_context.getPhysicalDevice(), queueRequirements, extensions.toArray(String[]::new), features.address(), null, true);
        }
        this.m_graphicsQueue = this.m_logicalDevice.generatedQueues.get(0);
        this.m_presentQueue = this.m_logicalDevice.generatedQueues.get(1);
        this.m_transferQueue = this.m_logicalDevice.generatedQueues.get(2);

        this.m_swapchain = new Swapchain(this.m_logicalDevice.get());
        this.m_swapchain.initialize(this.m_context, this.m_graphicsQueue.family(), this.m_presentQueue.family(), true);

        final int max_msaa_samples = VulkanHelpers.getMaxUsableSampleCount(this.m_logicalDevice.getBoundPhysicalDevice().properties());
        final int m_MSAA_sampleCount = Math.min(g_MSAA_sampleCount, max_msaa_samples);
        gRendererLogger.info(String.format("MSAA sample count : %d | Device supports : %d", m_MSAA_sampleCount, max_msaa_samples));

        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            this.m_depthImage = this.m_logicalDevice.getAllocationHelper().createImage(allocator,
                    this.m_swapchain.getWidth(), this.m_swapchain.getHeight(),
                    VK_FORMAT_D32_SFLOAT, VK_IMAGE_TILING_OPTIMAL, VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT, false,
                    1, m_MSAA_sampleCount, VK_IMAGE_ASPECT_DEPTH_BIT,
                    VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
            this.m_colorImage = this.m_logicalDevice.getAllocationHelper().createImage(allocator,
                    this.m_swapchain.getWidth(), this.m_swapchain.getHeight(),
                    this.m_swapchain.getSurfaceFormat().format(), VK_IMAGE_TILING_OPTIMAL,
                    VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
                    false, 1, m_MSAA_sampleCount, VK_IMAGE_ASPECT_COLOR_BIT,
                    VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY, VK_COMPONENT_SWIZZLE_IDENTITY,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
        }
        this.m_renderPass = new RenderPass(this.m_logicalDevice.get(), this.m_swapchain.getSurfaceFormat().format(),
                this.m_swapchain.getWidth(), this.m_swapchain.getHeight(), Arrays.stream(this.m_swapchain.getImages()).mapToLong(VulkanImage::imageView).toArray(),
                this.m_colorImage.imageView(), this.m_depthImage.imageView(), m_MSAA_sampleCount);

        this.m_planarShaders[0] = loadShaderModule(this.m_logicalDevice.get(), "planar-vs.glsl", VK_SHADER_STAGE_VERTEX_BIT);
        this.m_planarShaders[1] = loadShaderModule(this.m_logicalDevice.get(), "planar-fs.glsl", VK_SHADER_STAGE_FRAGMENT_BIT);
        this.m_gridShaders[0] = loadShaderModule(this.m_logicalDevice.get(), "grid-vs.glsl", VK_SHADER_STAGE_VERTEX_BIT);
        this.m_gridShaders[1] = loadShaderModule(this.m_logicalDevice.get(), "grid-fs.glsl", VK_SHADER_STAGE_FRAGMENT_BIT);
        this.m_scenePipeline = new GraphicsPipeline(this.m_logicalDevice.get(), this.m_renderPass.get(), new GraphicsPipeline.Description(
                new GraphicsPipeline.VertexShaderStage(this.m_planarShaders[0], new GraphicsPipeline.VertexShaderStage.InputData[] {
                        new GraphicsPipeline.VertexShaderStage.InputData(7 * Float.BYTES, true)
                }, new GraphicsPipeline.VertexShaderStage.Attribute[] {
                        new GraphicsPipeline.VertexShaderStage.Attribute(0, VK_FORMAT_R32G32B32_SFLOAT, 0),
                        new GraphicsPipeline.VertexShaderStage.Attribute(0, VK_FORMAT_R32G32B32A32_SFLOAT, 3 * Float.BYTES)
                }), this.m_planarShaders[1],
                null, null, null, new GraphicsPipeline.PushConstants[] {
                        new GraphicsPipeline.PushConstants(VK_SHADER_STAGE_VERTEX_BIT, 0, 32 * Float.BYTES)
                }, new Pipeline.Uniform[0],
                Either.ofRight(1), Either.ofRight(1), m_MSAA_sampleCount
        ));
        this.m_gridPipeline = new GraphicsPipeline(this.m_logicalDevice.get(), this.m_renderPass.get(), new GraphicsPipeline.Description(
                new GraphicsPipeline.VertexShaderStage(this.m_gridShaders[0], new GraphicsPipeline.VertexShaderStage.InputData[] {
                        new GraphicsPipeline.VertexShaderStage.InputData(2 * Float.BYTES, true)
                }, new GraphicsPipeline.VertexShaderStage.Attribute[] {
                        new GraphicsPipeline.VertexShaderStage.Attribute(0, VK_FORMAT_R32G32_SFLOAT, 0)
                }), this.m_gridShaders[1],
                null, null, null, new GraphicsPipeline.PushConstants[] {
                        new GraphicsPipeline.PushConstants(VK_SHADER_STAGE_VERTEX_BIT, 0, 32 * Float.BYTES)
                }, new Pipeline.Uniform[0],
                Either.ofRight(1), Either.ofRight(1), m_MSAA_sampleCount
        ));

        this.m_commandPool = new CommandPool(this.m_logicalDevice.get(), VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT, this.m_graphicsQueue.family());
        this.m_commandBuffers = this.m_commandPool.allocate(gFrameCount, VK_COMMAND_BUFFER_LEVEL_PRIMARY);
        this.m_syncObjects = new VulkanSync(this.m_logicalDevice.get(), gFrameCount);
        this.m_uploadCommandPool = new CommandPool(this.m_logicalDevice.get(), 0, this.m_transferQueue.family());

        try (FrameAllocator allocator = FrameAllocator.takeAndPush(); MemorySession bloat_session = MemorySession.openConfined())
        {
            /*MemorySegment vertices = bloat_session.allocateArray(ValueLayout.JAVA_FLOAT,
                    -0.5f, 0.5f, 0.f, 1.f, 0.f, 0.f, 1.f,
                    0.5f, 0.5f, 0.f, 0.f, 1.f, 0.f, 1.f,
                    0.f, 1.5f, 0.f, 0.f, 0.f, 1.f, 1.f);*/
            OBJModelLoader loader = new NativeOBJLoader(new OBJModelLoader.Settings(true, false));
            OBJModel model = loader.parse(Thread.currentThread().getContextClassLoader().getResourceAsStream("models/testarossa.obj"));
            OBJModel.Mesh main_mesh = model.meshes().get("Testarossa_Cube");
            MemorySegment vertices = bloat_session.allocateArray(ValueLayout.JAVA_FLOAT, main_mesh.vertices().length * 7L);
            for (int i = 0; i < main_mesh.vertices().length; i++)
            {
                vertices.setAtIndex(ValueLayout.JAVA_FLOAT, (long)i * 7, main_mesh.vertices()[i].x());
                vertices.setAtIndex(ValueLayout.JAVA_FLOAT, (long)i * 7 + 1, main_mesh.vertices()[i].y());
                vertices.setAtIndex(ValueLayout.JAVA_FLOAT, (long)i * 7 + 2, main_mesh.vertices()[i].z());
                final float f = (float)Math.random();
                vertices.setAtIndex(ValueLayout.JAVA_FLOAT, (long)i * 7 + 3, (float)Math.tan(f));
                vertices.setAtIndex(ValueLayout.JAVA_FLOAT, (long)i * 7 + 4, 1.f - (float)Math.sin(f));
                vertices.setAtIndex(ValueLayout.JAVA_FLOAT, (long)i * 7 + 5, (float)Math.cos(f));
                vertices.setAtIndex(ValueLayout.JAVA_FLOAT, (long)i * 7 + 6, 1.f);
            }
            MemorySegment indices = bloat_session.allocateArray(ValueLayout.JAVA_INT, main_mesh.faces().length * 3L);
            for (int i = 0; i < main_mesh.faces().length; i++)
            {
                indices.setAtIndex(ValueLayout.JAVA_INT, (long)i * 3, main_mesh.faces()[i].v0());
                indices.setAtIndex(ValueLayout.JAVA_INT, (long)i * 3 + 1, main_mesh.faces()[i].v1());
                indices.setAtIndex(ValueLayout.JAVA_INT, (long)i * 3 + 2, main_mesh.faces()[i].v2());
            }
            this.m_bunnyIndexCount = (int)indices.byteSize() / Integer.BYTES;

            this.m_vertexBuffer = this.m_logicalDevice.getAllocationHelper().createBuffer(allocator, vertices.byteSize(), VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT, new int[]{this.m_graphicsQueue.family()}, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
            this.m_vertexBuffer.upload(allocator, this.m_uploadCommandPool, this.m_transferQueue, this.m_logicalDevice.getAllocationHelper(), vertices.address().toRawLongValue(), vertices.byteSize());

            this.m_indexBuffer = this.m_logicalDevice.getAllocationHelper().createBuffer(allocator, indices.byteSize(), VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT, new int[]{this.m_graphicsQueue.family()}, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
            this.m_indexBuffer.upload(allocator, this.m_uploadCommandPool, this.m_transferQueue, this.m_logicalDevice.getAllocationHelper(), indices.address().toRawLongValue(), indices.byteSize());
        }

        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            MemorySegment vertices = allocator.allocateArray(ValueLayout.JAVA_FLOAT,
                    -1.f, -1.f,
                    1.f, -1.f,
                    1.f, 1.f,
                    -1.f, 1.f);
            this.m_gridVertexBuffer = this.m_logicalDevice.getAllocationHelper().createBuffer(allocator, vertices.byteSize(), VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT, new int[]{this.m_graphicsQueue.family()}, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
            this.m_gridVertexBuffer.upload(allocator, this.m_uploadCommandPool, this.m_transferQueue, this.m_logicalDevice.getAllocationHelper(), vertices.address().toRawLongValue(), vertices.byteSize());
            MemorySegment indices = allocator.allocateArray(ValueLayout.JAVA_INT,
                    0, 1, 2,
                    0, 2, 3,
                    2, 1, 0,
                    3, 2, 0);
            this.m_gridIndexBuffer = this.m_logicalDevice.getAllocationHelper().createBuffer(allocator, indices.byteSize(), VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT, new int[]{this.m_graphicsQueue.family()}, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
            this.m_gridIndexBuffer.upload(allocator, this.m_uploadCommandPool, this.m_transferQueue, this.m_logicalDevice.getAllocationHelper(), indices.address().toRawLongValue(), indices.byteSize());
        }

        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(1, allocator)
                    .apply(0, size -> size
                            .descriptorCount(1)
                            .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER));
            //this.m_gridDescriptorPool = new DescriptorPool(this.m_logicalDevice.get(), poolSizes, 1);
            //this.m_gridDescriptorSet = new DescriptorSet(this.m_logicalDevice.get(), this.m_gridDescriptorPool, this.m_gridPipeline.getDescriptorSetLayout());

            //this.m_gridUniformBuffer = this.m_logicalDevice.getAllocationHelper().createBuffer(allocator, 3 * Float.BYTES, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, new int[]{this.m_graphicsQueue.family()}, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        }
    }

    private void renderGrid(MemoryStack stack, VkCommandBuffer commandBuffer, VkViewport.Buffer viewports, VkRect2D.Buffer scissors, Camera camera)
    {
        vkCmdBindVertexBuffers(commandBuffer, 0, stack.longs(this.m_gridVertexBuffer.get()), stack.longs(0L));
        vkCmdBindIndexBuffer(commandBuffer, this.m_gridIndexBuffer.get(), 0L, VK_INDEX_TYPE_UINT32);
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, this.m_gridPipeline.get());
        vkCmdSetViewport(commandBuffer, 0, viewports);
        vkCmdSetScissor(commandBuffer, 0, scissors);

        FloatBuffer constantsBuffer = stack.mallocFloat(32);
        camera.getProjection(constantsBuffer);
        camera.getModelView(constantsBuffer.slice(16, 16), new kdMatrix4().identity());
        vkCmdPushConstants(commandBuffer, this.m_gridPipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, constantsBuffer);

        //constantsBuffer.limit(3);
        //constantsBuffer.put(0, new float[]{0.69f, 0.964f, 1.f});
        //this.m_gridUniformBuffer.put(stack, MemoryUtil.memAddress(constantsBuffer), 3 * Float.BYTES);
        //this.m_gridDescriptorSet.updateUniformBuffer(this.m_gridUniformBuffer.get(), 3 * Float.BYTES, 0);

        vkCmdDrawIndexed(commandBuffer, 12, 1, 0, 0, 0);
    }

    private void renderScene(MemoryStack stack, VkCommandBuffer commandBuffer, VkViewport.Buffer viewports, VkRect2D.Buffer scissors, Camera camera)
    {
        vkCmdBindVertexBuffers(commandBuffer, 0, stack.longs(this.m_vertexBuffer.get()), stack.longs(0L));
        vkCmdBindIndexBuffer(commandBuffer, this.m_indexBuffer.get(), 0L, VK_INDEX_TYPE_UINT32);
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, this.m_scenePipeline.get());
        vkCmdSetViewport(commandBuffer, 0, viewports);
        vkCmdSetScissor(commandBuffer, 0, scissors);

        FloatBuffer constantsBuffer = stack.callocFloat(32);
        camera.getProjection(constantsBuffer);
        kdMatrix4 modelMatrix = new kdMatrix4().identity();
        camera.getModelView(constantsBuffer.slice(16, 16), modelMatrix);
        vkCmdPushConstants(commandBuffer, this.m_scenePipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, constantsBuffer);

        vkCmdDrawIndexed(commandBuffer, this.m_bunnyIndexCount, 1, 0, 0, 0);
    }

    public void renderFrame(final Camera camera)
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            LongBuffer pCurrentFence = allocator.longs(this.m_syncObjects.m_fences[this.m_currentFrame]);
            VulkanException.check(vkWaitForFences(this.m_logicalDevice.get(), pCurrentFence, true, Long.MAX_VALUE));
            VulkanException.check(vkResetFences(this.m_logicalDevice.get(), pCurrentFence));

            IntBuffer pFrameIndex = allocator.mallocInt(1);
            VulkanException.check(this.m_swapchain.acquireNextImage(this.m_syncObjects.m_imageAcquiredSemaphores[this.m_currentFrame], pFrameIndex));

            VulkanException.check(vkResetCommandBuffer(this.m_commandBuffers[this.m_currentFrame], 0));
            VulkanHelpers.beginCommandBuffer(allocator, this.m_commandBuffers[this.m_currentFrame], 0);

            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(this.m_renderPass.get())
                    .renderArea(area -> area.extent(extent -> extent.set(this.m_swapchain.getWidth(), this.m_swapchain.getHeight())))
                    .clearValueCount(2)
                    .pClearValues(VkClearValue.calloc(2, allocator)
                            .apply(0, clear_value -> clear_value
                                    .color(c -> c.float32().put(0, new float[]{0.f, 0.f, 0.f, 1.f})))
                            .apply(1, clear_value -> clear_value
                                    .depthStencil(c -> c.set(1.f, 0))))
                    .framebuffer(this.m_renderPass.getFramebuffers()[pFrameIndex.get(0)]);
            vkCmdBeginRenderPass(this.m_commandBuffers[this.m_currentFrame], renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);

            VkViewport.Buffer pViewports = VkViewport.calloc(1, allocator)
                    .apply(0, viewport -> viewport
                            .set(0.f, this.m_swapchain.getHeight(), (float)this.m_swapchain.getWidth(), (float)-this.m_swapchain.getHeight(), 0.f, 1.f));
            VkRect2D.Buffer pScissors = VkRect2D.calloc(1, allocator)
                    .apply(0, scissor -> scissor
                            .extent(e -> e.set(this.m_swapchain.getWidth(), this.m_swapchain.getHeight())));

            this.renderScene(allocator, this.m_commandBuffers[this.m_currentFrame], pViewports, pScissors, camera);
            this.renderGrid(allocator, this.m_commandBuffers[this.m_currentFrame], pViewports, pScissors, camera);

            vkCmdEndRenderPass(this.m_commandBuffers[this.m_currentFrame]);
            VulkanException.check(vkEndCommandBuffer(this.m_commandBuffers[this.m_currentFrame]));

            LongBuffer pRenderCompleteSemaphore = allocator.longs(this.m_syncObjects.m_renderCompleteSemaphores[this.m_currentFrame]);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .waitSemaphoreCount(1)
                    .pWaitSemaphores(allocator.longs(this.m_syncObjects.m_imageAcquiredSemaphores[this.m_currentFrame]))
                    .pWaitDstStageMask(allocator.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                    .pCommandBuffers(allocator.pointers(this.m_commandBuffers[this.m_currentFrame]))
                    .pSignalSemaphores(pRenderCompleteSemaphore);
            VulkanException.check(vkQueueSubmit(this.m_graphicsQueue.handle(), submitInfo, this.m_syncObjects.m_fences[this.m_currentFrame]));

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(allocator)
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .pWaitSemaphores(pRenderCompleteSemaphore)
                    .swapchainCount(1)
                    .pSwapchains(allocator.longs(this.m_swapchain.get()))
                    .pImageIndices(pFrameIndex);
            VulkanException.check(KHRSwapchain.vkQueuePresentKHR(this.m_presentQueue.handle(), presentInfo));
        }

        this.m_currentFrame = (this.m_currentFrame + 1) % gFrameCount;
    }

    public void destroy()
    {
        VulkanException.check(vkDeviceWaitIdle(this.m_logicalDevice.get()));

        this.m_gridVertexBuffer.free();
        this.m_gridIndexBuffer.free();
        this.m_vertexBuffer.free();
        this.m_indexBuffer.free();

        this.m_syncObjects.dispose();

        this.m_uploadCommandPool.dispose();
        this.m_commandPool.dispose();

        //this.m_gridUniformBuffer.free();
        //this.m_gridDescriptorSet.dispose();
        //this.m_gridDescriptorPool.dispose();
        this.m_gridPipeline.dispose();
        this.m_scenePipeline.dispose();

        for (ShaderModule module : this.m_planarShaders) module.dispose();
        for (ShaderModule module : this.m_gridShaders) module.dispose();

        this.m_renderPass.dispose();
        this.m_depthImage.free();
        this.m_colorImage.free();
        this.m_swapchain.dispose();
        this.m_logicalDevice.dispose();
        this.m_context.dispose();
    }
}
