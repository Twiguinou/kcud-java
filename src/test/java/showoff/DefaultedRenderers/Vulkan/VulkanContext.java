package showoff.DefaultedRenderers.Vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;
import showoff.Disposable;
import showoff.FrameAllocator;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BinaryOperator;

import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.EXTValidationFeatures.*;
import static org.lwjgl.vulkan.EXTDebugUtils.*;

public class VulkanContext implements Disposable
{
    private static final String[] gDebugValidationLayers = new String[] {
            "VK_LAYER_LUNARG_standard_validation",
            "VK_LAYER_KHRONOS_validation"
    };

    public record PhysicalDevice(VkPhysicalDevice handle, VkPhysicalDeviceFeatures features, VkPhysicalDeviceProperties properties, VkPhysicalDeviceMemoryProperties memoryProperties)
    {
        public boolean isDedicated()
        {
            return this.properties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU;
        }
        private void free()
        {
            this.features.free();
            this.properties.free();
            this.memoryProperties.free();
        }
    }

    private VkInstance m_instance;
    private PhysicalDevice m_physicalDevice;
    private long m_debugMessengerHandle;

    public VulkanContext()
    {
        this.m_instance = null;
        this.m_physicalDevice = null;
        this.m_debugMessengerHandle = VK_NULL_HANDLE;
    }

    public static int DefaultCallbackFunction(int messageSeverity, int messageTypes, long pCallbackData, long pUserData)
    {
        VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
        if (messageSeverity >= VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT)
        {
            System.err.println(callbackData.pMessageString());
        }
        else if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT) == 0)
        {
            System.out.println(callbackData.pMessageString());
        }
        return VK_FALSE;
    }

    public void initialize(String app_name, int app_version, String engine_name, int engine_version, String[] requiredExtensions, VkDebugUtilsMessengerCallbackEXTI debug_callback) throws VulkanException
    {
        if (this.m_instance != null) this.dispose();
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            VkApplicationInfo applicationInfo = VkApplicationInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .pApplicationName(allocator.UTF8(app_name))
                    .applicationVersion(app_version)
                    .pEngineName(allocator.UTF8(engine_name))
                    .engineVersion(engine_version)
                    .apiVersion(VK_API_VERSION_1_2);

            final boolean stacktrace = debug_callback != null;
            VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(applicationInfo);
            List<PointerBuffer> extensions = new LinkedList<>();
            {
                IntBuffer pExtensionCount = allocator.mallocInt(1);
                VulkanException.check(vkEnumerateInstanceExtensionProperties((ByteBuffer)null, pExtensionCount, null));
                if (pExtensionCount.get(0) > 0)
                {
                    VkExtensionProperties.Buffer pExtensionProperties = VkExtensionProperties.malloc(pExtensionCount.get(0), allocator);
                    VulkanException.check(vkEnumerateInstanceExtensionProperties((ByteBuffer)null, pExtensionCount, pExtensionProperties));
                    success: for (final String ext : requiredExtensions)
                    {
                        if (stacktrace && ext.equals(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)) continue;
                        for (int j = 0; j < pExtensionProperties.capacity(); j++)
                        {
                            final VkExtensionProperties extensionProperties = pExtensionProperties.get(j);
                            if (ext.equals(extensionProperties.extensionNameString()))
                            {
                                System.out.println("Enabling vulkan extension: " + ext);
                                extensions.add(PointerBuffer.create(extensionProperties.extensionName()));
                                continue success;
                            }
                        }
                        throw new VulkanException("Could not fetch vulkan extension: " + ext);
                    }
                }
            }
            List<PointerBuffer> validationLayers = new LinkedList<>();
            if (stacktrace)
            {
                IntBuffer pPropertyCount = allocator.mallocInt(1);
                VulkanException.check(vkEnumerateInstanceLayerProperties(pPropertyCount, null));
                if (pPropertyCount.get(0) > 0)
                {
                    VkLayerProperties.Buffer pLayerProperties = VkLayerProperties.malloc(pPropertyCount.get(0), allocator);
                    VulkanException.check(vkEnumerateInstanceLayerProperties(pPropertyCount, pLayerProperties));
                    for (final String layer : gDebugValidationLayers)
                    {
                        for (int j = 0; j < pLayerProperties.capacity(); j++)
                        {
                            final VkLayerProperties layerProperties = pLayerProperties.get(j);
                            if (layer.equals(layerProperties.layerNameString()))
                            {
                                System.out.println("Enabling validation layer: " + layer);
                                validationLayers.add(PointerBuffer.create(layerProperties.layerName()));
                                break;
                            }
                        }
                    }
                }
                extensions.add(PointerBuffer.create(allocator.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)));
                instanceCreateInfo.pNext(VkValidationFeaturesEXT.calloc(allocator)
                        .sType(VK_STRUCTURE_TYPE_VALIDATION_FEATURES_EXT)
                        .pEnabledValidationFeatures(allocator.ints(VK_VALIDATION_FEATURE_ENABLE_BEST_PRACTICES_EXT, VK_VALIDATION_FEATURE_ENABLE_SYNCHRONIZATION_VALIDATION_EXT)));
            }
            instanceCreateInfo.ppEnabledLayerNames(validationLayers.isEmpty() ? null : allocator.pointers(validationLayers.toArray(PointerBuffer[]::new)));
            instanceCreateInfo.ppEnabledExtensionNames(extensions.isEmpty() ? null : allocator.pointers(extensions.toArray(PointerBuffer[]::new)));

            PointerBuffer pInstance = allocator.mallocPointer(1);
            VulkanException.check(vkCreateInstance(instanceCreateInfo, null, pInstance), "Instance creation failed.");
            this.m_instance = new VkInstance(pInstance.get(0), instanceCreateInfo);

            if (stacktrace)
            {
                VkDebugUtilsMessengerCreateInfoEXT messengerCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(allocator)
                        .sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                        .messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT| VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                                VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT | VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                        .messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
                        .pfnUserCallback(debug_callback);
                LongBuffer pCallback = allocator.mallocLong(1);
                VulkanException.check(vkCreateDebugUtilsMessengerEXT(this.m_instance, messengerCreateInfo, null, pCallback), "Debug messenger creation failed");
                this.m_debugMessengerHandle = pCallback.get(0);
            }
        }
    }

    public VkInstance getInstance()
    {
        return this.m_instance;
    }

    public PhysicalDevice getPhysicalDevice()
    {
        return this.m_physicalDevice;
    }

    public void findSuitableDevice(BinaryOperator<PhysicalDevice> comparator) throws VulkanException
    {
        if (this.m_instance == null) return;
        try (FrameAllocator allocator = FrameAllocator.takeAndPushIfEmpty())
        {
            IntBuffer pDeviceCount = allocator.mallocInt(1);
            VulkanException.check(vkEnumeratePhysicalDevices(this.m_instance, pDeviceCount, null));
            if (pDeviceCount.get(0) > 0)
            {
                PointerBuffer pPhysicalDevices = allocator.mallocPointer(pDeviceCount.get(0));
                VulkanException.check(vkEnumeratePhysicalDevices(this.m_instance, pDeviceCount, pPhysicalDevices));
                List<PhysicalDevice> physicalDevices = new LinkedList<>();
                for (int i = 0; i < pPhysicalDevices.capacity(); i++)
                {
                    VkPhysicalDevice physicalDevice = new VkPhysicalDevice( pPhysicalDevices.get(i), this.m_instance);
                    VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.malloc();
                    vkGetPhysicalDeviceProperties(physicalDevice, properties);
                    VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.malloc();
                    vkGetPhysicalDeviceFeatures(physicalDevice, features);
                    VkPhysicalDeviceMemoryProperties memoryProperties = VkPhysicalDeviceMemoryProperties.malloc();
                    vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);
                    physicalDevices.add(new PhysicalDevice(physicalDevice, features, properties, memoryProperties));
                }

                PhysicalDevice selected = physicalDevices.stream().reduce(comparator).orElse(null);
                if (physicalDevices.remove(selected))
                {
                    this.m_physicalDevice = selected;
                }

                physicalDevices.forEach(PhysicalDevice::free);
            }
        }
    }

    @Override
    public void dispose()
    {
        if (this.m_physicalDevice != null)
        {
            this.m_physicalDevice.free();
            this.m_physicalDevice = null;
        }
        if (this.m_debugMessengerHandle != VK_NULL_HANDLE)
        {
            vkDestroyDebugUtilsMessengerEXT(this.m_instance, this.m_debugMessengerHandle, null);
            this.m_debugMessengerHandle = VK_NULL_HANDLE;
        }
        vkDestroyInstance(this.m_instance, null);
        this.m_instance = null;
    }
}
