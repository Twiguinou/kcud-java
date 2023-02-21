package showoff.App.Feature;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;

public abstract class OBJModelLoader
{
    protected static final Logger gLoaderLogger = LogManager.getLogger("Integrated OBJ model loader");
    public record Settings(boolean triangulate, boolean flip_uvs) {}
    public static class Error extends RuntimeException
    {
        protected Error(Exception e) {super(e);}
        protected Error(String s) {super(s);}
    }

    protected final Settings m_settings;

    protected OBJModelLoader(Settings settings)
    {
        this.m_settings = settings;
    }

    public abstract OBJModel parse(InputStream input) throws Error;
}
