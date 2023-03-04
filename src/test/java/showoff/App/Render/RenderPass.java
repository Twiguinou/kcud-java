package showoff.App.Render;

import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;
import showoff.Vulkan.VulkanException;
import showoff.Disposable;
import showoff.FrameAllocator;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK13.*;

public class RenderPass implements Disposable
{
    private final long m_internalHandle;
    private long[] m_framebuffers;
    private final VkDevice device;

    public RenderPass(VkDevice device, int format, int msaa_sample_count) throws VulkanException
    {
        this.device = device;
        this.m_framebuffers = new long[0];
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            VkAttachmentDescription.Buffer pAttachments = VkAttachmentDescription.calloc(3, allocator)
                    .apply(0, description -> description
                            .format(format)
                            .samples(msaa_sample_count)
                            .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                            .storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                            .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_LOAD)
                            .stencilStoreOp(VK_ATTACHMENT_STORE_OP_STORE)
                            .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                            .finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL))
                    .apply(1, description -> description
                            .format(VK_FORMAT_D32_SFLOAT)
                            .samples(msaa_sample_count)
                            .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                            .storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                            .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                            .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                            .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                            .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL))
                    .apply(2, description -> description
                            .format(format)
                            .samples(VK_SAMPLE_COUNT_1_BIT)
                            .loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                            .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                            .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                            .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                            .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                            .finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR));

            VkAttachmentReference.Buffer pColorAttachments = VkAttachmentReference.calloc(1, allocator)
                    .apply(0, reference -> reference
                            .attachment(0)
                            .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL));
            VkAttachmentReference depthAttachmentReference = VkAttachmentReference.calloc(allocator)
                    .attachment(1)
                    .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            VkAttachmentReference.Buffer pResolveAttachments = VkAttachmentReference.calloc(1, allocator)
                    .apply(0, reference -> reference
                            .attachment(2)
                            .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL));

            VkSubpassDescription.Buffer pSubpasses = VkSubpassDescription.calloc(1, allocator)
                    .apply(0, description -> description
                            .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                            .colorAttachmentCount(1)
                            .pColorAttachments(pColorAttachments)
                            .pDepthStencilAttachment(depthAttachmentReference)
                            .pResolveAttachments(pResolveAttachments));

            VkSubpassDependency.Buffer pDependencies = VkSubpassDependency.calloc(1, allocator)
                    .srcSubpass(VK_SUBPASS_EXTERNAL)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT)
                    .srcAccessMask(0)
                    .dstSubpass(0)
                    .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT)
                    .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT)
                    .dependencyFlags(0);

            VkRenderPassCreateInfo renderPassCreateInfo = VkRenderPassCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pAttachments(pAttachments)
                    .pSubpasses(pSubpasses)
                    .pDependencies(pDependencies);
            LongBuffer pRenderPass = allocator.mallocLong(1);
            VulkanException.check(vkCreateRenderPass(this.device, renderPassCreateInfo, null, pRenderPass));
            this.m_internalHandle = pRenderPass.get(0);
        }
    }
    
    public void createFramebuffers(long[] imageViews, int width, int height, long msaaImageView, long depthImageView) throws VulkanException
    {
    	long[] fbs = new long[imageViews.length];
    	int i = -1;
    	try (FrameAllocator allocator = FrameAllocator.takeAndPush())
    	{
    		LongBuffer pFramebufferAttachments = allocator.longs(msaaImageView, depthImageView, 0);
            VkFramebufferCreateInfo framebufferCreateInfo = VkFramebufferCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    .renderPass(this.m_internalHandle)
                    .attachmentCount(3)
                    .pAttachments(pFramebufferAttachments)
                    .width(width)
                    .height(height)
                    .layers(1);
            LongBuffer pFramebuffer = allocator.mallocLong(1);
            for (i = 0; i < imageViews.length; i++)
            {
                pFramebufferAttachments.put(2, imageViews[i]);
                VulkanException.check(vkCreateFramebuffer(this.device, framebufferCreateInfo, null, pFramebuffer));
                fbs[i] = pFramebuffer.get(0);
            }
            this.m_framebuffers = fbs;
    	}
    	catch (VulkanException e)
    	{
    		for (; i >= 0; i--)
    		{
    			vkDestroyFramebuffer(this.device, fbs[i], null);
    		}
    		throw e;
    	}
    }

    public long get()
    {
        return this.m_internalHandle;
    }

    public long[] getFramebuffers()
    {
        return this.m_framebuffers;
    }
    
    protected void destroyFramebuffers()
    {
    	for (long framebuffer : this.m_framebuffers)
    	{
    		vkDestroyFramebuffer(this.device, framebuffer, null);
    	}
    	this.m_framebuffers = new long[0];
    }

    @Override
    public void dispose()
    {
    	this.destroyFramebuffers();
        vkDestroyRenderPass(this.device, this.m_internalHandle, null);
    }
}
