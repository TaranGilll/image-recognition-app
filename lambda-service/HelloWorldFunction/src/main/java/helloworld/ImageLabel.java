package helloworld;

import java.util.List;

public class ImageLabel {
    private String label;
    private String confidence;
    private List<String> parentLabels;

    public ImageLabel() {
        this.label = "";
        this.confidence = "";
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getConfidence() {
        return this.confidence;
    }

    public void setParentLabels(List<String> parentLabels) {
        this.parentLabels = parentLabels;
    }

    public List<String> getParentLabels() {
        return this.parentLabels;
    }
}
