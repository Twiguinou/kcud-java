package showoff.WindowContext;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkInstance;

import javax.annotation.Nullable;
import java.awt.event.KeyEvent;
import java.nio.LongBuffer;
import java.security.Key;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;

public class GLFWWindowProcessor implements WindowProcessor
{
    private long m_handle;
    private int m_width, m_height;
    private String m_title;
    private WndSizeCallback m_sizeCallback = null;
    private WndKeyInputCallback m_keyCallback = null;
    private WndMouseButtonCallback m_mouseButtonCallback = null;
    private WndMousePosCallback m_mousePosCallback = null;
    private WndMouseWheelCallback m_mouseWheelCallback = null;
    private GLFWErrorCallback m_errorCallback = null;

    public GLFWWindowProcessor()
    {
        this.m_handle = MemoryUtil.NULL;
        this.m_width = 0;
        this.m_height = 0;
        this.m_title = "";
    }

    private static int mapKeyboardInput(int glfw_input)
    {
        return switch (glfw_input) {
            case GLFW_KEY_APOSTROPHE -> KeyEvent.VK_QUOTE;
            case GLFW_KEY_BACKSPACE -> KeyEvent.VK_BACK_SPACE;
            case GLFW_KEY_CAPS_LOCK -> KeyEvent.VK_CAPS_LOCK;
            case GLFW_KEY_DELETE -> KeyEvent.VK_DELETE;
            case GLFW_KEY_DOWN -> KeyEvent.VK_DOWN;
            case GLFW_KEY_UP -> KeyEvent.VK_UP;
            case GLFW_KEY_LEFT -> KeyEvent.VK_LEFT;
            case GLFW_KEY_RIGHT -> KeyEvent.VK_RIGHT;
            case GLFW_KEY_END -> KeyEvent.VK_END;
            case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> KeyEvent.VK_ENTER;
            case GLFW_KEY_ESCAPE -> KeyEvent.VK_ESCAPE;
            case GLFW_KEY_F1 -> KeyEvent.VK_F1;
            case GLFW_KEY_F2 -> KeyEvent.VK_F2;
            case GLFW_KEY_F3 -> KeyEvent.VK_F3;
            case GLFW_KEY_F4 -> KeyEvent.VK_F4;
            case GLFW_KEY_F5 -> KeyEvent.VK_F5;
            case GLFW_KEY_F6 -> KeyEvent.VK_F6;
            case GLFW_KEY_F7 -> KeyEvent.VK_F7;
            case GLFW_KEY_F8 -> KeyEvent.VK_F8;
            case GLFW_KEY_F9 -> KeyEvent.VK_F9;
            case GLFW_KEY_F10 -> KeyEvent.VK_F10;
            case GLFW_KEY_F11 -> KeyEvent.VK_F11;
            case GLFW_KEY_F12 -> KeyEvent.VK_F12;
            case GLFW_KEY_F13 -> KeyEvent.VK_F13;
            case GLFW_KEY_F14 -> KeyEvent.VK_F14;
            case GLFW_KEY_F15 -> KeyEvent.VK_F15;
            case GLFW_KEY_F16 -> KeyEvent.VK_F16;
            case GLFW_KEY_F17 -> KeyEvent.VK_F17;
            case GLFW_KEY_F18 -> KeyEvent.VK_F18;
            case GLFW_KEY_F19 -> KeyEvent.VK_F19;
            case GLFW_KEY_F20 -> KeyEvent.VK_F20;
            case GLFW_KEY_F21 -> KeyEvent.VK_F21;
            case GLFW_KEY_F22 -> KeyEvent.VK_F22;
            case GLFW_KEY_F23 -> KeyEvent.VK_F23;
            case GLFW_KEY_F24 -> KeyEvent.VK_F24;
            case GLFW_KEY_GRAVE_ACCENT -> KeyEvent.VK_BACK_QUOTE;
            case GLFW_KEY_HOME -> KeyEvent.VK_HOME;
            case GLFW_KEY_INSERT -> KeyEvent.VK_INSERT;
            case GLFW_KEY_KP_0 -> KeyEvent.VK_NUMPAD0;
            case GLFW_KEY_KP_1 -> KeyEvent.VK_NUMPAD1;
            case GLFW_KEY_KP_2 -> KeyEvent.VK_NUMPAD2;
            case GLFW_KEY_KP_3 -> KeyEvent.VK_NUMPAD3;
            case GLFW_KEY_KP_4 -> KeyEvent.VK_NUMPAD4;
            case GLFW_KEY_KP_5 -> KeyEvent.VK_NUMPAD5;
            case GLFW_KEY_KP_6 -> KeyEvent.VK_NUMPAD6;
            case GLFW_KEY_KP_7 -> KeyEvent.VK_NUMPAD7;
            case GLFW_KEY_KP_8 -> KeyEvent.VK_NUMPAD8;
            case GLFW_KEY_KP_9 -> KeyEvent.VK_NUMPAD9;
            case GLFW_KEY_KP_ADD -> KeyEvent.VK_ADD;
            case GLFW_KEY_KP_DECIMAL -> KeyEvent.VK_DECIMAL;
            case GLFW_KEY_KP_EQUAL -> KeyEvent.VK_EQUALS;
            case GLFW_KEY_KP_MULTIPLY -> KeyEvent.VK_MULTIPLY;
            case GLFW_KEY_KP_SUBTRACT -> KeyEvent.VK_DIVIDE;
            case GLFW_KEY_LEFT_ALT, GLFW_KEY_RIGHT_ALT -> KeyEvent.VK_ALT;
            case GLFW_KEY_LEFT_BRACKET -> KeyEvent.VK_OPEN_BRACKET;
            case GLFW_KEY_RIGHT_BRACKET -> KeyEvent.VK_CLOSE_BRACKET;
            case GLFW_KEY_LEFT_CONTROL, GLFW_KEY_RIGHT_CONTROL -> KeyEvent.VK_CONTROL;
            case GLFW_KEY_LEFT_SHIFT, GLFW_KEY_RIGHT_SHIFT -> KeyEvent.VK_SHIFT;
            case GLFW_KEY_MINUS -> KeyEvent.VK_MINUS;
            case GLFW_KEY_NUM_LOCK -> KeyEvent.VK_NUM_LOCK;
            case GLFW_KEY_PAGE_DOWN -> KeyEvent.VK_PAGE_DOWN;
            case GLFW_KEY_PAGE_UP -> KeyEvent.VK_PAGE_UP;
            case GLFW_KEY_PAUSE -> KeyEvent.VK_PAUSE;
            case GLFW_KEY_PRINT_SCREEN -> KeyEvent.VK_PRINTSCREEN;
            case GLFW_KEY_SCROLL_LOCK -> KeyEvent.VK_SCROLL_LOCK;
            case GLFW_KEY_TAB -> KeyEvent.VK_TAB;
            default -> glfw_input;
        };
    }

    @Override
    public boolean createWindowContext(int width, int height)
    {
        if (!glfwInit()) return false;
        if (this.m_handle != MemoryUtil.NULL) endWindowContext();

        this.m_errorCallback = GLFWErrorCallback.createPrint(System.err).set();
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        final GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        final int scaled_width, scaled_height;
        if (vidMode != null)
        {
            scaled_width = width * vidMode.width() / 1920;
            scaled_height = height * vidMode.height() / 1080;
        }
        else
        {
            scaled_width = width;
            scaled_height = height;
        }
        this.m_handle = glfwCreateWindow(scaled_width, scaled_height, this.m_title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (this.m_handle == MemoryUtil.NULL) return false;

        glfwSetFramebufferSizeCallback(this.m_handle, (__, w, h) ->
        {
            this.m_width = w;
            this.m_height = h;
            if (this.m_sizeCallback != null) this.m_sizeCallback.invoke(this.m_width, this.m_height);
        });
        glfwSetCursorPosCallback(this.m_handle, (__, x, y) ->
        {
            if (this.m_mousePosCallback != null) this.m_mousePosCallback.invoke(x, y);
        });
        glfwSetMouseButtonCallback(this.m_handle, (__, button, action, mods) ->
        {
            if (this.m_mouseButtonCallback != null) this.m_mouseButtonCallback.invoke(button, action == GLFW_PRESS ? MouseButtonAction.PRESS : MouseButtonAction.RELEASE, mods);
        });
        glfwSetKeyCallback(this.m_handle, (__, key, scancode, action, mods) ->
        {
            if (this.m_keyCallback == null) return;
            final KeyInputState inputState = switch (action)
                    {
                        case GLFW_REPEAT -> KeyInputState.REPEAT;
                        case GLFW_PRESS -> KeyInputState.PRESS;
                        default -> KeyInputState.RELEASE;
                    };
            this.m_keyCallback.invoke(mapKeyboardInput(key), scancode, inputState, mods);
        });
        glfwSetScrollCallback(this.m_handle, (__, xo, yo) ->
        {
            if (this.m_mouseWheelCallback != null) this.m_mouseWheelCallback.invoke(xo, yo);
        });

        this.m_width = width;
        this.m_height = height;
        glfwSetWindowPos(this.m_handle, (vidMode.width() - scaled_width) / 2, (vidMode.height() - scaled_height) / 2);
        glfwShowWindow(this.m_handle);
        return true;
    }

    public long getHandle()
    {
        return this.m_handle;
    }

    @Override
    public void endWindowContext()
    {
        Callbacks.glfwFreeCallbacks(this.m_handle);
        glfwDestroyWindow(this.m_handle);
        this.m_handle = MemoryUtil.NULL;
        this.m_errorCallback.free();
        this.m_errorCallback = null;
    }

    @Override public int getWidth() {return this.m_width;}
    @Override public int getHeight() {return this.m_height;}
    @Override public String getWindowTitle() {return this.m_title;}
    @Override public void setWindowTitle(String title) {this.m_title = title;}

    @Override
    public void beginRenderStage()
    {
        glfwPollEvents();
    }

    @Override
    public void endRenderStage() {}

    @Override
    public boolean windowShouldClose()
    {
        return glfwWindowShouldClose(this.m_handle);
    }

    @Override public void setWndSizeCallback(@Nullable WndSizeCallback callback)
    {
        this.m_sizeCallback = callback;
    }
    @Override public void setWndMousePosCallback(@Nullable WndMousePosCallback callback)
    {
        this.m_mousePosCallback = callback;
    }
    @Override public void setWndMouseButtonCallback(@Nullable WndMouseButtonCallback callback)
    {
        this.m_mouseButtonCallback = callback;
    }
    @Override public void setWndMouseWheelCallback(@Nullable WndMouseWheelCallback callback)
    {
        this.m_mouseWheelCallback = callback;
    }
    @Override public void setWndKeyInputCallback(@Nullable WndKeyInputCallback callback)
    {
        this.m_keyCallback = callback;
    }

    @Override
    public String[] getVulkanExtensions()
    {
        PointerBuffer glfw_extensions = glfwGetRequiredInstanceExtensions();
        if (glfw_extensions == null) return new String[]{};
        String[] result = new String[glfw_extensions.capacity()];
        for (int i = 0; i < glfw_extensions.capacity(); i++) result[i] = glfw_extensions.getStringUTF8(i);
        return result;
    }

    @Override
    public int createVulkanSurface(VkInstance instance, VkAllocationCallbacks allocator, LongBuffer dest)
    {
        return glfwCreateWindowSurface(instance, this.m_handle, allocator, dest);
    }
}
