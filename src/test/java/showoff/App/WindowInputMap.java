package showoff.App;

import org.lwjgl.glfw.GLFW;
import showoff.WindowContext.WindowProcessor;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.BiConsumer;

public class WindowInputMap
{
    private final WindowProcessor.KeyInputState[] m_keyInputs;
    private final WindowProcessor.MouseButtonAction[] m_mouseButtons;
    private double m_mousePosX = 0.d, m_mousePosY = 0.d;
    private double m_mousePosX_prev = 0.d, m_mousePosY_prev = 0.d;

    public WindowInputMap()
    {
        this.m_keyInputs = new WindowProcessor.KeyInputState[GLFW.GLFW_KEY_LAST + 1];
        Arrays.fill(this.m_keyInputs, WindowProcessor.KeyInputState.RELEASE);
        this.m_mouseButtons = new WindowProcessor.MouseButtonAction[GLFW.GLFW_MOUSE_BUTTON_LAST + 1];
        Arrays.fill(this.m_mouseButtons, WindowProcessor.MouseButtonAction.RELEASE);
    }

    public void registerCallbacks(WindowProcessor windowProc, @Nullable WindowProcessor.WndMouseWheelCallback wheelCallback, @Nullable BiConsumer<Double, Double> mouseOffsetCallback)
    {
        windowProc.setWndKeyInputCallback((key, scancode, action, mods) ->
        {
            if (key != GLFW.GLFW_KEY_UNKNOWN)
            {
                this.m_keyInputs[key] = action;
            }
        });
        windowProc.setWndMouseWheelCallback(wheelCallback);
        windowProc.setWndMousePosCallback((xpos, ypos) ->
        {
            this.m_mousePosX_prev = this.m_mousePosX;
            this.m_mousePosY_prev = this.m_mousePosY;
            this.m_mousePosX = xpos;
            this.m_mousePosY = ypos;
            if (mouseOffsetCallback != null)
            {
                mouseOffsetCallback.accept(this.m_mousePosX - this.m_mousePosX_prev, this.m_mousePosY - this.m_mousePosY_prev);
            }
        });
        windowProc.setWndMouseButtonCallback((button, action, mods) -> this.m_mouseButtons[button] = action);
    }

    public WindowProcessor.KeyInputState get(int key)
    {
        return this.m_keyInputs[key];
    }

    public WindowProcessor.MouseButtonAction getMouse(int key)
    {
        return this.m_mouseButtons[key];
    }
    public double getMouseX() {return this.m_mousePosX;}
    public double getMouseY() {return this.m_mousePosY;}
}
