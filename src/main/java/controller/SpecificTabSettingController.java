package controller;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;

public class SpecificTabSettingController implements Initializable {
    @FXML
    private TreeView<String> treeView;

    @Override
    public void initialize(java.net.URL url, java.util.ResourceBundle bundle) {
        TreeItem<String> rootItem = new TreeItem<>("root");
        TreeItem<String> server = new CheckBoxTreeItem<>("hogehoge.example.com");
        server.getChildren().addAll(
            Stream.of("Home", "Local", "Notification")
                .map(CheckBoxTreeItem<String>::new).collect(Collectors.toList())
        );
        server.setExpanded(true);
        treeView.setCellFactory((TreeView<String> p) -> new CheckBoxTreeCell<String>());
        rootItem.getChildren().add(server);
        treeView.setRoot(rootItem);
        treeView.setShowRoot(false);

    }
}
