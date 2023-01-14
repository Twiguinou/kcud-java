package showoff.DefaultedRenderers.Vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import showoff.ForeignStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.EXTValidationFeatures.*;

public class VulkanContext
{
    public record PhysicalDevice(
            VkPhysicalDevice handle,
            VkPhysicalDeviceMemoryProperties memProps,
            VkPhysicalDeviceFeatures features,
            VkPhysicalDeviceProperties properties
    ) {}

    private VkInstance m_instance;
    public final List<String> m_validatedExtensions;
    public final List<PhysicalDevice> m_detectedDevices;
    private long m_debugCallbackHandle;

    public VulkanContext()
    {
        this.m_instance = null;
        this.m_validatedExtensions = new ArrayList<>();
        this.m_detectedDevices = new ArrayList<>();
        this.m_debugCallbackHandle = MemoryUtil.NULL;
    }

    public int initialize(String[] requiredExtensions, boolean useValidationLayers, VkDebugUtilsMessengerCallbackEXTI debug_callback)
    {
        if (this.m_instance != null)
        {
            this.disposeContext();
        }

        try (ForeignStack stack = ForeignStack.pushConfined())
        {
            VkApplicationInfo applicationInfo = VkApplicationInfo.calloc(stack);
            applicationInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
            applicationInfo.pNext(MemoryUtil.NULL);
            applicationInfo.pApplicationName(stack.UTF8("showoff"));
            applicationInfo.applicationVersion(0);
            applicationInfo.pEngineName(null);
            applicationInfo.engineVersion(0);
            applicationInfo.apiVersion(VK_API_VERSION_1_2);

            IntBuffer presentExtensionCount = stack.mallocInt(1);
            assert vkEnumerateInstanceExtensionProperties((ByteBuffer)null, presentExtensionCount, null) == VK_SUCCESS;

            VkExtensionProperties.Buffer extensionPropertiesList = VkExtensionProperties.malloc(presentExtensionCount.get(0), stack);
            assert vkEnumerateInstanceExtensionProperties((ByteBuffer)null, presentExtensionCount, extensionPropertiesList) == VK_SUCCESS;
            for (int i = 0; i < requiredExtensions.length; i++)
            {
                for (int j = 0; j < presentExtensionCount.get(0); i++)
                {
                    if (requiredExtensions[i].equals(extensionPropertiesList.get(j).extensionNameString()))
                    {
                        System.out.printf("Enabling Vulkan extension: %s\n", requiredExtensions[i]);
                        this.m_validatedExtensions.add(requiredExtensions[i]);
                        break;
                    }
                }
            }

            VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.calloc(stack);
            instanceCreateInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            instanceCreateInfo.pNext(MemoryUtil.NULL);
            instanceCreateInfo.flags(0);
            instanceCreateInfo.pApplicationInfo(applicationInfo);
            List<String> validationLayers = new LinkedList<>();
            if (useValidationLayers)
            {
                IntBuffer propertyCount = stack.mallocInt(1);
                assert vkEnumerateInstanceLayerProperties(propertyCount, null) == VK_SUCCESS;
                if (propertyCount.get(0) != 0)
                {
                    validationLayers.add("VK_LAYER_LUNARG_standard_validation");
                    validationLayers.add("VK_LAYER_KHRONOS_validation");
                    VkLayerProperties.Buffer layerPropertiesList = VkLayerProperties.malloc(propertyCount.get(0), stack);
                    next_layer: for (int i = 0; i < validationLayers.size();)
                    {
                        for (int j = 0; j < propertyCount.get(0); j++)
                        {
                            if (validationLayers.get(i).equals(layerPropertiesList.get(j).layerNameString()))
                            {
                                System.out.printf("Enabling validation layer: %s\n", validationLayers.get(i));
                                continue next_layer;
                            }
                        }
                        validationLayers.remove(i);
                    }
                }
                this.m_validatedExtensions.add(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
                IntBuffer enabledValidationFeatures = stack.ints(VK_VALIDATION_FEATURE_ENABLE_BEST_PRACTICES_EXT, VK_VALIDATION_FEATURE_ENABLE_SYNCHRONIZATION_VALIDATION_EXT);

                VkValidationFeaturesEXT validationFeatures = VkValidationFeaturesEXT.calloc(stack);
                validationFeatures.sType(VK_STRUCTURE_TYPE_VALIDATION_FEATURES_EXT);
                validationFeatures.pNext(MemoryUtil.NULL);
                validationFeatures.pEnabledValidationFeatures(enabledValidationFeatures);
                validationFeatures.pDisabledValidationFeatures(null);
                instanceCreateInfo.pNext(validationFeatures);
            }
            instanceCreateInfo.ppEnabledLayerNames(stack.UTF8_list(validationLayers));
            instanceCreateInfo.ppEnabledExtensionNames(stack.UTF8_list(this.m_validatedExtensions));
            PointerBuffer pInstance = stack.mallocPointer(1);
            int vk_result = vkCreateInstance(instanceCreateInfo, null, pInstance);
            if (vk_result != VK_SUCCESS)
            {
                System.err.println("Instance creation failed.");
                return vk_result;
            }
            this.m_instance = new VkInstance(pInstance.get(0), instanceCreateInfo);

            if (useValidationLayers && debug_callback != null)
            {
                VkDebugUtilsMessengerCreateInfoEXT messengerCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                messengerCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
                messengerCreateInfo.pNext(MemoryUtil.NULL);
                messengerCreateInfo.flags(0);
                messengerCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT
                );
                messengerCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
                );
                messengerCreateInfo.pfnUserCallback(debug_callback);
                messengerCreateInfo.pUserData(MemoryUtil.NULL);
                LongBuffer pMessenger = stack.mallocLong(1);
                vk_result = vkCreateDebugUtilsMessengerEXT(this.m_instance, messengerCreateInfo, null, pMessenger);
                if (vk_result != VK_SUCCESS)
                {
                    System.err.println("Debug messenger creation failed");
                    return vk_result;
                }
                this.m_debugCallbackHandle = pMessenger.get(0);
            }

            return VK_SUCCESS;
        }
    }

    public void disposeContext()
    {
        assert this.m_instance != null;
        if (this.m_debugCallbackHandle != MemoryUtil.NULL)
        {
            vkDestroyDebugUtilsMessengerEXT(this.m_instance, this.m_debugCallbackHandle, null);
            this.m_debugCallbackHandle = MemoryUtil.NULL;
        }
        vkDestroyInstance(this.m_instance, null);
        this.m_instance = null;
        this.m_validatedExtensions.clear();
        this.m_detectedDevices.clear();
    }

    public VkInstance getVulkanInstance()
    {
        return this.m_instance;
    }

    public void detectPresentDevices()
    {
        this.m_detectedDevices.clear();
        try (ForeignStack stack = ForeignStack.pushConfined())
        {
            IntBuffer pDeviceCount = stack.mallocInt(1);
            assert vkEnumeratePhysicalDevices(this.m_instance, pDeviceCount, null) == VK_SUCCESS;
            if (pDeviceCount.get(0) == 0)
            {
                return;
            }

            PointerBuffer physicalDevices = stack.mallocPointer(pDeviceCount.get(0));
            assert vkEnumeratePhysicalDevices(this.m_instance, pDeviceCount, physicalDevices) == VK_SUCCESS;
            for (int i = 0; i < pDeviceCount.get(0); i++)
            {
                PhysicalDevice wrapper = new PhysicalDevice(
                        new VkPhysicalDevice(physicalDevices.get(i), this.m_instance),
                        VkPhysicalDeviceMemoryProperties.create(),
                        VkPhysicalDeviceFeatures.create(),
                        VkPhysicalDeviceProperties.create()
                );
                vkGetPhysicalDeviceMemoryProperties(wrapper.handle, wrapper.memProps);
                vkGetPhysicalDeviceFeatures(wrapper.handle, wrapper.features);
                vkGetPhysicalDeviceProperties(wrapper.handle, wrapper.properties);
                this.m_detectedDevices.add(wrapper);
            }
        }
    }

}
