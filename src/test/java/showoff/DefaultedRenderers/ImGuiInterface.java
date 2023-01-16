package showoff.DefaultedRenderers;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.type.ImInt;
import org.lwjgl.vulkan.VkBufferImageCopy;
import showoff.ForeignStack;

import java.nio.ByteBuffer;

public class ImGuiInterface
{

    public void createFontTexture(long command_pool)
    {
        ImGuiIO io = ImGui.getIO();
        ImInt width = new ImInt(), height = new ImInt();
        ByteBuffer data = ImGui.getIO().getFonts().getTexDataAsRGBA32(width, height);
        int data_size = width.get() * height.get() * 4;

        ImGui.getIO().getFonts().setTexID(-1);
        try (ForeignStack stack = ForeignStack.pushConfined(VkBufferImageCopy.SIZEOF))
        {
            VkBufferImageCopy bufferImageCopy = VkBufferImageCopy.calloc(stack);
        }
    }

}
