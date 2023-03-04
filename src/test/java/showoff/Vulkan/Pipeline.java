package showoff.Vulkan;

import org.lwjgl.vulkan.VkDevice;
import showoff.Disposable;

import static org.lwjgl.vulkan.VK13.*;

public abstract class Pipeline implements Disposable
{
    public record PushConstants(int stages, int offset, int size) {}

    protected final VkDevice device;

    protected Pipeline(VkDevice device)
    {
        this.device = device;
    }

    public abstract long get();
    public abstract long getLayout();

    @Override
    public void dispose()
    {
        vkDestroyPipeline(this.device, this.get(), null);
        vkDestroyPipelineLayout(this.device, this.getLayout(), null);
    }
}
