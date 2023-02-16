package showoff.DefaultedRenderers.Vulkan;

public interface VulkanImage
{
    long get();
    long imageView();
    void free();

    static VulkanImage wrap(long handle, long imageView)
    {
        return new VulkanImage()
        {
            @Override public long get()
            {
                return handle;
            }
            @Override public long imageView()
            {
                return imageView;
            }
            @Override public void free() {}
        };
    }

    interface MemoryBound extends VulkanImage
    {
        long memory();
    }
}
