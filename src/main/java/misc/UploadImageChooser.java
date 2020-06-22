package misc;

import javafx.stage.FileChooser;

import java.io.File;
import java.util.Map;

public class UploadImageChooser {
    public static void choose(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("アップロード画像を選択");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("画像ファイル", "*.jpg", "*.png", "*.gif"),
                new FileChooser.ExtensionFilter("すべてのファイル", "*.*")
        );
        File file = fileChooser.showOpenDialog(null); // TODO: stage渡す

        if(file != null) {
            var mimeTypes = Map.of(
                "jpg", "image/jpeg",
                "png", "image.png",
                "gif", "image/gif"
            );

            System.out.println(file.getPath());
            String[] strings = file.getPath().split("\\.");
            String ext = strings[strings.length - 1];
            String mimeType = mimeTypes.get(ext);
            System.out.println(mimeType);
        }
    }
}
