package bootanimation.zip.bootzipcreator.models;

public class BootAnimation {

    private int id;
    private String videoPath;
    private String zipPath;
    private String videoFileName;
    private String zipFileName;
    private long creationTime;

    public BootAnimation() {
    }

    public BootAnimation(String videoPath, String zipPath, String videoFileName, String zipFileName, long creationTime) {
        this.videoPath = videoPath;
        this.zipPath = zipPath;
        this.videoFileName = videoFileName;
        this.zipFileName = zipFileName;
        this.creationTime = creationTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public String getZipPath() {
        return zipPath;
    }

    public void setZipPath(String zipPath) {
        this.zipPath = zipPath;
    }

    public String getVideoFileName() {
        return videoFileName;
    }

    public void setVideoFileName(String videoFileName) {
        this.videoFileName = videoFileName;
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public void setZipFileName(String zipFileName) {
        this.zipFileName = zipFileName;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }
}
