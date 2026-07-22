package com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.raw.pojo;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Debug;

@Debug.Renderer(text = "name + \" <- \" + parent")
public class Bone {
    @SerializedName("cubes")
    private Cube[] cubes;
    @SerializedName("debug")
    private Boolean debug;
    @SerializedName("inflate")
    private Float inflate;
    @SerializedName("mirror")
    private Boolean mirror;
    @SerializedName("name")
    private String name;
    @SerializedName("parent")
    private String parent;
    @SerializedName("pivot")
    private float[] pivot = new float[]{0, 0, 0};
    @SerializedName("reset")
    private Boolean reset;
    @SerializedName("rotation")
    private float[] rotation = new float[]{0, 0, 0};


    public Cube[] getCubes() {
        return cubes;
    }

    public void setCubes(Cube[] value) {
        this.cubes = value;
    }

    public Boolean getDebug() {
        return debug;
    }

    public void setDebug(Boolean value) {
        this.debug = value;
    }

    public Float getInflate() {
        return inflate;
    }

    public void setInflate(Float value) {
        this.inflate = value;
    }

    public Boolean getMirror() {
        return mirror;
    }

    public void setMirror(Boolean value) {
        this.mirror = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String value) {
        this.parent = value;
    }

    public float[] getPivot() {
        return pivot;
    }

    public void setPivot(float[] value) {
        this.pivot = value;
    }

    public Boolean getReset() {
        return reset;
    }

    public void setReset(Boolean value) {
        this.reset = value;
    }

    public float[] getRotation() {
        return rotation;
    }

    public void setRotation(float[] value) {
        this.rotation = value;
    }


}
