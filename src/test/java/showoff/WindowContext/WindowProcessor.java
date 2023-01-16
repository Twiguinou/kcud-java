package showoff.WindowContext;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkInstance;

import java.nio.LongBuffer;

public interface WindowProcessor
{

    boolean createWindowContext(int width, int height);

    void endWindowContext();

    int getWidth();
    int getHeight();

    String getWindowTitle();
    void setWindowTitle(String title);

    void beginRenderStage();
    void endRenderStage();

    boolean windowShouldClose();

    PointerBuffer getVulkanExtensions();
    int createVulkanSurface(VkInstance instance, final VkAllocationCallbacks allocator, LongBuffer dest);

}
