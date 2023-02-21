package showoff.App.Render;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import showoff.App.ObjectMesh;
import showoff.DefaultedRenderers.Vulkan.LogicalDevice;
import showoff.DefaultedRenderers.Vulkan.ext.Swapchain;
import showoff.DefaultedRenderers.Vulkan.ext.VulkanRenderContext;
import showoff.WindowContext.GLFWWindowProcessor;
import showoff.WindowContext.WindowProcessor;

public class VulkanSceneRenderer implements SceneRenderer
{
    private static final Logger gRendererLogger = LogManager.getLogger("Vulkan Renderer");
    private static final int gFrameCount = 2;

    private final VulkanRenderContext m_vulkanContext = null;
    private final WindowProcessor m_windowProc = new GLFWWindowProcessor();
    private final LogicalDevice m_logicalDevice = null;
    private record Queues(int graphicsQueue, int presentQueue) {}
    private final Swapchain m_swapchain = null;

    @Override
    public void attachInterface()
    {
    }

    @Override
    public int registerPipeline()
    {
        return 0;
    }

    @Override
    public int registerMesh(ObjectMesh mesh)
    {
        return 0;
    }

    @Override
    public int addInstancingGroup(int mesh_identifier, int max_object_count)
    {
        return 0;
    }

    @Override
    public void destroy()
    {
    }
}
