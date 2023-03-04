package showoff.App.Render.scene;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;
import showoff.App.Render.Camera;

public interface SceneObjectRenderer
{
    void render(MemoryStack renderStack, VkCommandBuffer commandBuffer, VkViewport.Buffer viewports, VkRect2D.Buffer scissors, Camera camera, int frameIndex);
    void destroy();
}
