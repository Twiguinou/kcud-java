package showoff.WindowContext;

import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkInstance;
import showoff.Disposable;

import java.nio.LongBuffer;

public interface WindowProcessor extends Disposable
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

    String[] getVulkanExtensions();
    int createVulkanSurface(VkInstance instance, final VkAllocationCallbacks allocator, LongBuffer dest);

    @Override
    default void dispose()
    {
        this.endWindowContext();
    }
}
