package showoff;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import showoff.DefaultedRenderers.Vulkan.CommandBuffers;
import showoff.DefaultedRenderers.Vulkan.LogicalDevice;
import showoff.DefaultedRenderers.Vulkan.VulkanContext;
import showoff.DefaultedRenderers.Vulkan.VulkanException;
import showoff.DefaultedRenderers.Vulkan.ext.Swapchain;
import showoff.DefaultedRenderers.Vulkan.ext.VulkanRenderContext;
import showoff.WindowContext.GLFWWindowProcessor;
import showoff.WindowContext.WindowProcessor;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK12.*;

public class Main
{
    public static void main(String... args)
    {
        try (DisposableStack disposables = new DisposableStack())
        {
            WindowProcessor windowProc = disposables.addAndGet(new GLFWWindowProcessor());
            windowProc.setWindowTitle("showoff");
            windowProc.createWindowContext(1280, 960);

            VulkanRenderContext context = new VulkanRenderContext();
            context.initialize("showoff", 0, "kcud", 0, windowProc.getVulkanExtensions(), VulkanContext::DefaultCallbackFunction);
            disposables.add(context);
            context.attachSurface(windowProc);
            context.findSuitableDevice((first, second) ->
            {
                int i = 0;
                if (first.isDedicated()) ++i;
                if (second.isDedicated()) --i;
                return i > 0 ? first : second;
            });
            System.out.println("Selected physical device: " + context.getPhysicalDevice().properties().deviceNameString());

            LogicalDevice logicalDevice = new LogicalDevice();
            try (FrameAllocator allocator = FrameAllocator.openConfined(VkPhysicalDeviceFeatures.SIZEOF * 2))
            {
                LogicalDevice.QueueRequirements[] queueRequirements = new LogicalDevice.QueueRequirements[]
                        {
                                new LogicalDevice.QueueRequirements(VK12.VK_QUEUE_GRAPHICS_BIT, new long[]{context.getSurface()}, null, false)
                        };
                VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.calloc(allocator);
                logicalDevice.initialize(context.getPhysicalDevice(), queueRequirements, new String[]{Swapchain.getSwapchainExtensionName()}, MemoryUtil.NULL, features, true);
            }
            disposables.add(logicalDevice);

            Swapchain swapchain = new Swapchain(logicalDevice.get());
            swapchain.initialize(context, logicalDevice.generatedQueues.get(0).family(), logicalDevice.generatedQueues.get(0).family(), true);
            disposables.add(swapchain);
            swapchain.createImageViews();

            final long renderPass;
            try (FrameAllocator allocator = FrameAllocator.takeAndPush())
            {
                VkAttachmentDescription.Buffer pAttachmentDescriptions = VkAttachmentDescription.calloc(1, allocator);
                pAttachmentDescriptions.get(0)
                        .format(swapchain.getSurfaceFormat().format())
                        .samples(VK_SAMPLE_COUNT_1_BIT)
                        .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                        .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                        .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                        .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                        .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                        .finalLayout(Swapchain.getPresentSourceLayout());

                VkAttachmentReference.Buffer pColorAttachments = VkAttachmentReference.calloc(1, allocator);
                pColorAttachments.get(0)
                        .attachment(0)
                        .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
                VkSubpassDescription.Buffer pSubpassDescriptions = VkSubpassDescription.calloc(1, allocator);
                pSubpassDescriptions.get(0)
                        .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                        .colorAttachmentCount(1)
                        .pColorAttachments(pColorAttachments);

                VkSubpassDependency.Buffer pDependencies = VkSubpassDependency.calloc(1, allocator);
                pDependencies.get(0)
                        .srcSubpass(VK_SUBPASS_EXTERNAL)
                        .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                        .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                        .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                        .dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT);

                VkRenderPassCreateInfo renderPassCreateInfo = VkRenderPassCreateInfo.calloc(allocator)
                        .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                        .pAttachments(pAttachmentDescriptions)
                        .pSubpasses(pSubpassDescriptions)
                        .pDependencies(pDependencies);
                LongBuffer pRenderPass = allocator.mallocLong(1);
                VulkanException.check(vkCreateRenderPass(logicalDevice.get(), renderPassCreateInfo, null, pRenderPass), "Failed to create render pass.");
                renderPass = pRenderPass.get(0);
            }
            disposables.add(() -> vkDestroyRenderPass(logicalDevice.get(), renderPass, null));

            long[] framebuffers = new long[swapchain.getImageViews().length];
            try (FrameAllocator allocator = FrameAllocator.takeAndPush())
            {
                LongBuffer pAttachments = allocator.mallocLong(1);
                VkFramebufferCreateInfo framebufferCreateInfo = VkFramebufferCreateInfo.calloc(allocator)
                        .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                        .pAttachments(pAttachments)
                        .width(swapchain.getWidth())
                        .height(swapchain.getHeight())
                        .layers(1)
                        .renderPass(renderPass);
                LongBuffer pFramebuffer = allocator.mallocLong(1);
                for (int i = 0; i < framebuffers.length; i++)
                {
                    pAttachments.put(0, swapchain.getImageViews()[i]);
                    VulkanException.check(vkCreateFramebuffer(logicalDevice.get(), framebufferCreateInfo, null, pFramebuffer));
                    framebuffers[i] = pFramebuffer.get(0);
                    final int fi = i;
                    disposables.add(() -> vkDestroyFramebuffer(logicalDevice.get(), framebuffers[fi], null));
                }
            }

            long commandPool = CommandBuffers.createPool(logicalDevice.get(), 0, logicalDevice.generatedQueues.get(0).family());
            disposables.add(() -> CommandBuffers.destroyPool(logicalDevice.get(), commandPool));
            VkCommandBuffer[] rasterCommandBuffers = CommandBuffers.allocate(logicalDevice.get(), commandPool, swapchain.getImageViews().length, VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            disposables.add(() ->
            {
                for (VkCommandBuffer commandBuffer : rasterCommandBuffers)
                {
                    CommandBuffers.free(logicalDevice.get(), commandPool, commandBuffer);
                }
            });
            try (FrameAllocator allocator = FrameAllocator.takeAndPush())
            {
                VkClearValue.Buffer pClearValues = VkClearValue.calloc(1, allocator);
                pClearValues.get(0).color().float32(allocator.floats(0.6f, 0.8f, 0.6f, 1.f));
                VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(allocator)
                        .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                        .renderPass(renderPass)
                        .pClearValues(pClearValues)
                        .renderArea(a -> a.extent().set(swapchain.getWidth(), swapchain.getHeight()));
                VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc(allocator)
                        .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
                for (int i = 0; i < swapchain.getImageViews().length; i++)
                {
                    VulkanException.check(vkBeginCommandBuffer(rasterCommandBuffers[i], commandBufferBeginInfo));
                    renderPassBeginInfo.framebuffer(framebuffers[i]);
                    vkCmdBeginRenderPass(rasterCommandBuffers[i], renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);
                    vkCmdEndRenderPass(rasterCommandBuffers[i]);
                    VulkanException.check(vkEndCommandBuffer(rasterCommandBuffers[i]));
                }
            }

            long[] imageAcquireSemaphores = new long[swapchain.getImageViews().length];
            long[] rasterCompleteSemaphores = new long[swapchain.getImageViews().length];
            long[] renderFences = new long[swapchain.getImageViews().length];
            try (FrameAllocator allocator = FrameAllocator.takeAndPush())
            {
                LongBuffer pVkDest = allocator.mallocLong(1);
                VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(allocator)
                        .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
                VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.calloc(allocator)
                        .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                        .flags(VK_FENCE_CREATE_SIGNALED_BIT);
                for (int i = 0; i < swapchain.getImageViews().length; i++)
                {
                    final int fi = i;
                    VulkanException.check(vkCreateSemaphore(logicalDevice.get(), semaphoreCreateInfo, null, pVkDest));
                    imageAcquireSemaphores[i] = pVkDest.get(0);
                    disposables.add(() -> vkDestroySemaphore(logicalDevice.get(), imageAcquireSemaphores[fi], null));

                    VulkanException.check(vkCreateSemaphore(logicalDevice.get(), semaphoreCreateInfo, null, pVkDest));
                    rasterCompleteSemaphores[i] = pVkDest.get(0);
                    disposables.add(() -> vkDestroySemaphore(logicalDevice.get(), rasterCompleteSemaphores[fi], null));

                    VulkanException.check(vkCreateFence(logicalDevice.get(), fenceCreateInfo, null, pVkDest));
                    renderFences[i] = pVkDest.get(0);
                    disposables.add(() -> vkDestroyFence(logicalDevice.get(), renderFences[fi], null));
                }
            }

            try (FrameAllocator allocator = FrameAllocator.takeAndPush())
            {
                IntBuffer pFrameIndex = allocator.mallocInt(1);

                LongBuffer pWaitSemaphores1 = allocator.mallocLong(1);
                PointerBuffer pCommandBuffers = allocator.mallocPointer(1);
                LongBuffer pSignalSemaphores = allocator.mallocLong(1);
                VkSubmitInfo submitInfo = VkSubmitInfo.calloc(allocator)
                        .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                        .pWaitSemaphores(pWaitSemaphores1)
                        .pWaitDstStageMask(allocator.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                        .pCommandBuffers(pCommandBuffers)
                        .waitSemaphoreCount(1)
                        .pSignalSemaphores(pSignalSemaphores);

                LongBuffer pWaitSemaphores2 = allocator.mallocLong(1);
                VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(allocator)
                        .sType$Default()
                        .pWaitSemaphores(pWaitSemaphores2)
                        .swapchainCount(1)
                        .pSwapchains(allocator.longs(swapchain.getHandle()))
                        .pImageIndices(pFrameIndex);

                int frame = 0;
                while (!windowProc.windowShouldClose())
                {
                    windowProc.beginRenderStage();
                    VulkanException.check(vkWaitForFences(logicalDevice.get(), renderFences[frame], true, Long.MAX_VALUE));
                    VulkanException.check(vkResetFences(logicalDevice.get(), renderFences[frame]));
                    swapchain.acquireNextImage(imageAcquireSemaphores[frame], pFrameIndex);

                    pWaitSemaphores1.put(0, imageAcquireSemaphores[frame]);
                    pCommandBuffers.put(0, rasterCommandBuffers[frame]);
                    pSignalSemaphores.put(0, rasterCompleteSemaphores[frame]);
                    VulkanException.check(vkQueueSubmit(logicalDevice.generatedQueues.get(0).handle(), submitInfo, renderFences[frame]));
                    pWaitSemaphores2.put(0, rasterCompleteSemaphores[frame]);
                    VulkanException.check(KHRSwapchain.vkQueuePresentKHR(logicalDevice.generatedQueues.get(0).handle(), presentInfo));

                    frame = (frame + 1) % swapchain.getImageViews().length;
                    windowProc.endRenderStage();
                }

                VulkanException.check(vkDeviceWaitIdle(logicalDevice.get()));
            }
        }
    }
}
