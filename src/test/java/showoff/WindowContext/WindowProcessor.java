package showoff.WindowContext;

import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkInstance;
import showoff.Disposable;

import javax.annotation.Nullable;
import java.nio.LongBuffer;

public interface WindowProcessor extends Disposable
{
    enum MouseButtonAction
    {
        PRESS,
        RELEASE
    }
    enum KeyInputState
    {
        PRESS,
        RELEASE,
        REPEAT
    }
    interface WndSizeCallback {void invoke(int width, int height);}
    interface WndMousePosCallback {void invoke(double xpos, double ypos);}
    interface WndMouseButtonCallback {void invoke(int button, MouseButtonAction action, int mods);}
    interface WndMouseWheelCallback {void invoke(double xm, double ym);}
    interface WndKeyInputCallback {void invoke(int key, int scancode, KeyInputState action, int mods);}

    boolean createWindowContext(int width, int height);

    void endWindowContext();

    int getWidth();
    int getHeight();

    String getWindowTitle();
    void setWindowTitle(String title);

    void beginRenderStage();
    void endRenderStage();

    boolean windowShouldClose();
    boolean isFocused();

    void setWndSizeCallback(@Nullable WndSizeCallback callback);
    void setWndMousePosCallback(@Nullable WndMousePosCallback callback);
    void setWndMouseButtonCallback(@Nullable WndMouseButtonCallback callback);
    void setWndMouseWheelCallback(@Nullable WndMouseWheelCallback callback);
    void setWndKeyInputCallback(@Nullable WndKeyInputCallback callback);

    String[] getVulkanExtensions();
    int createVulkanSurface(VkInstance instance, final VkAllocationCallbacks allocator, LongBuffer dest);

    @Override
    default void dispose()
    {
        this.endWindowContext();
    }
}
