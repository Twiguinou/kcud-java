package showoff.App.Feature;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JavaOBJLoader extends OBJModelLoader
{
    public JavaOBJLoader(Settings settings)
    {
        super(settings);
        if (this.m_settings.triangulate())
        {
            throw new UnsupportedOperationException("Only triangulated data can be ordered for parsing");
        }
    }

    public static OBJModel.Face tryParseFace(String[] tokens) throws Error
    {
        if (tokens.length != 4) throw new Error("Incompatible model format, not triangulated.");
        return new OBJModel.Face(
                Integer.parseInt(tokens[1].split("/")[0]),
                Integer.parseInt(tokens[2].split("/")[0]),
                Integer.parseInt(tokens[3].split("/")[0])
        );
    }

    public static OBJModel.Component3 tryParseVertex(String[] tokens) throws Error
    {
        if (tokens.length != 4 && tokens.length != 5) throw new Error("Syntax error, invalid number of vertex coordinates.");
        return new OBJModel.Component3(Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3]));
    }

    public static OBJModel.Component3 tryParseNormal(String[] tokens) throws Error
    {
        if (tokens.length != 4) throw new Error("Syntax error, invalid number of normal components.");
        return new OBJModel.Component3(Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3]));
    }

    public static OBJModel.Component2 tryParseUV(String[] tokens, boolean flip_vertical) throws Error
    {
        if (tokens.length == 2)
        {
            return new OBJModel.Component2(Float.parseFloat(tokens[1]), 0.f);
        }
        else if (tokens.length == 3 || tokens.length == 4)
        {
            return new OBJModel.Component2(Float.parseFloat(tokens[1]), flip_vertical ? 1.f - Float.parseFloat(tokens[2]) : Float.parseFloat(tokens[2]));
        }
        throw new Error("Syntax error, invalid number of uv components");
    }

    @Override
    public OBJModel parse(File file) throws Error
    {
        List<OBJModel.Mesh> meshes = new ArrayList<>();
        List<OBJModel.Component3> vertices = new ArrayList<>();
        List<OBJModel.Component3> normals = new ArrayList<>();
        List<OBJModel.Component2> uvs = new ArrayList<>();
        List<OBJModel.Face> faces = new ArrayList<>();
        try (BufferedReader file_io = new BufferedReader(new FileReader(file)))
        {
            String line;
            int i = 0;
            while ((line = file_io.readLine()) != null)
            {
                ++i;
                if (line.isBlank() || (line = line.trim()).charAt(0) == '#') continue;

                String[] tokens = line.split(" ");
                try
                {
                    switch (tokens[0])
                    {
                        case "v" -> vertices.add(tryParseVertex(tokens));
                        case "vn" -> normals.add(tryParseNormal(tokens));
                        case "vt" -> uvs.add(tryParseUV(tokens, this.m_settings.flip_uvs()));
                        case "f" -> faces.add(tryParseFace(tokens));
                    }
                }
                catch (Error e)
                {
                    throw new Error(String.format("Failed to parse obj model at line %d -> %s ", i, e));
                }
            }
        }
        catch (IOException e)
        {
            throw new Error(e);
        }

        meshes.add(new OBJModel.Mesh("", vertices.toArray(OBJModel.Component3[]::new), normals.toArray(OBJModel.Component3[]::new), uvs.toArray(OBJModel.Component2[]::new), faces.toArray(OBJModel.Face[]::new)));
        return new OBJModel(meshes, null);
    }
}
