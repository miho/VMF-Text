package eu.mihosoft.vmf.vmftext;

/**
 * Options that influence VMF-Text code generation.
 */
public final class GenerationOptions {

    private boolean autoLabel;
    private boolean emitAutoLabelReport = true;

    public GenerationOptions() {
        //
    }

    public static GenerationOptions defaults() {
        return new GenerationOptions();
    }

    public boolean isAutoLabel() {
        return autoLabel;
    }

    public GenerationOptions setAutoLabel(boolean autoLabel) {
        this.autoLabel = autoLabel;
        return this;
    }

    public boolean isEmitAutoLabelReport() {
        return emitAutoLabelReport;
    }

    public GenerationOptions setEmitAutoLabelReport(boolean emitAutoLabelReport) {
        this.emitAutoLabelReport = emitAutoLabelReport;
        return this;
    }
}
