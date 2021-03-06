package controller;

import connection.MastodonAPI;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Callback;

import misc.BrowserLauncher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;
import timeline.TimelineGenerator;
import timeline.parser.ITimelineGenerator;
import timeline.parser.MastodonWriteAPIParser;

import java.util.stream.Collectors;

import static timeline.parser.MastodonTimelineParser.validateURL;

public class TimelineViewController implements Initializable, IContentListController {
    @FXML
    private TableView<TimelineGenerator.RowContent> tableView;

    private Controller rootController;
    private ITimelineGenerator timelineGenerator;
    private MastodonAPI postMastodonAPI;

    String hostname;

    @FXML
    private TableColumn iconCol;

    @FXML private StackPane filterWordPane;
    @FXML private TextField filterWordField;

    public void userFilterWordBoxToggle(){
        ObservableList<String> styleClass = filterWordPane.getStyleClass();
        if(styleClass.contains("u-hidden")){
            styleClass.remove("u-hidden");
            filterWordField.requestFocus();
        }
        else {
            styleClass.add("u-hidden");
        }
    }


    public void iconInvisible(boolean value){
        if(value) {
            iconCol.getStyleClass().add("u-hidden");
        }
        else {
            iconCol.getStyleClass().remove("u-hidden");
        }
        // TODO: 初期化時にも処理したい
    }

    public void tableViewSetItems(ObservableList<TimelineGenerator.RowContent> rowContents){
        tableView.setItems(rowContents);
    }

    @Override
    public void reload() {
        ObservableList<TimelineGenerator.RowContent> rowContents = timelineGenerator.createRowContents(); // TODO:
        tableViewSetItems(rowContents);
        filterWord();
    }

    private void filterWord(){
        String filterWord = filterWordField.getText();
        ObservableList<TimelineGenerator.RowContent> rowContents = timelineGenerator.getRowContents();

        if( filterWord.isEmpty() ){
            tableViewSetItems(rowContents);
        }
        ObservableList<TimelineGenerator.RowContent> filteredRowContents = FXCollections.observableList(rowContents.stream().filter(rowContent -> rowContent.contentText.contains(filterWord)).collect(Collectors.toList()));
        tableViewSetItems(filteredRowContents);
    }

    public void viewRefresh(){
        reload();
    }

    public void registerParentControllerObject(Controller rootController, ITimelineGenerator timelineGenerator, MastodonAPI postMastodonAPI, String hostname){
        this.rootController = rootController;
        this.postMastodonAPI = postMastodonAPI;
        this.timelineGenerator = timelineGenerator;
        this.hostname = hostname;
    }

    public void registerWebViewOutput(WebView webView){
        final String twemoji = "<script src=\"https://twemoji.maxcdn.com/v/12.1.5/twemoji.min.js\" integrity=\"sha384-E4PZh8MWwKQ2W7ANni7xwx6TTuPWtd3F8mDRnaMvJssp5j+gxvP2fTsk1GnFg2gG\" crossorigin=\"anonymous\"></script>";
        final String styleString = "<style>html{font-size: 12px;background-color: #2B2B2B; color: #ffffff;font-family: Meiryo,\"メイリオ\",'Segoe UI Emoji',sans-serif;font-weight:500;}</style>";
        final String contentHeader = "<!DOCTYPE html><html lang=\"ja\"><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">" + twemoji + styleString + "</head><body><div class=\"ContentWrapper\">";
        // final String EMOJI_TEST = "<span style=\"border: 1px #cccccc solid;\">絵文字でねえ🍑💯 &#x1F004</span>";
        //final String EMOJI_TEST = "\uD842\uDFB7野屋";
        final String EMOJI_TEST = "";
        final String twemojiFooter = "<script>twemoji.parse(document.body)</script>";
        final String contentFooter = "<br></div>"+toCharacterReference(EMOJI_TEST)+twemojiFooter+"</body></html>";
        ObservableList selectedCells = tableView.getSelectionModel().getSelectedCells();
        if(selectedCells == null) return;

        selectedCells.addListener(new ListChangeListener() {
            @Override
            public void onChanged(Change c) {
                TimelineGenerator.RowContent tootContent = tableView.getSelectionModel().getSelectedItem();
                if(tootContent == null) return;
                StringBuffer imagesString = new StringBuffer();
                for(String imageURL : tootContent.contentImageURL) {
                    String contentImageElement = "<img src=\"" + imageURL + "\" />";
                    imagesString.append(contentImageElement);
                }
                String contentImageElement = "<div class=\"ContentImage\">" + imagesString.toString() + "</div>";
                String contentHtml = toCharacterReference(tootContent.contentHtml);
                String contentBodyElement = "<div class=\"ContentBody\">" + contentHtml + "</div>";
                String htmlString = contentHeader + contentBodyElement + contentImageElement + contentFooter;
                WebEngine webEngine = webView.getEngine();
                webEngine.setUserStyleSheetLocation(getClass().getResource("webview.css").toString());
                webEngine.loadContent(htmlString);
            }
        });
    }

    public static class TootCell extends TableRow<TimelineGenerator.RowContent> {
        @Override
        protected void updateItem(TimelineGenerator.RowContent rowContent, boolean empty){
            if( rowContent != null && "true".equals(rowContent.favorited) ) {
                this.getStyleClass().add("-favorited");
            }
            else{
                this.getStyleClass().remove("-favorited");
            }
            super.updateItem(rowContent, empty);
        }
    }

    private void contextMenuInit(){
        ContextMenu contextMenu = new ContextMenu();
        MenuItem menuItemFavorite = new MenuItem("お気に入り");
        MenuItem menuItemReblog = new MenuItem("リブログ");
        MenuItem menuItemUserTimeline = new MenuItem("このユーザーのタイムラインを見る");
        MenuItem menuItemReply = new MenuItem("返信");
        MenuItem menuItemStatusURL = new MenuItem("この投稿をブラウザで開く");
        MenuItem menuItemInfo = new MenuItem("情報");
        menuItemFavorite.setOnAction((ActionEvent t) -> {
            TimelineGenerator.RowContent selectedToot = tableView.getSelectionModel().getSelectedItem();
            String hostname = selectedToot.dataOriginInfo.hostname;
            String statusId = selectedToot.id;

            if( "mastodon".equals(selectedToot.dataOriginInfo.serverType) ) {
                MastodonWriteAPIParser mastodonWriteAPIParser = selectedToot.writeActionApi;
                mastodonWriteAPIParser.addFavorite(statusId); // TODO: 成功時、TimelineGeneratorの内部状態への反映
            }
        });

        menuItemReblog.setOnAction((ActionEvent t) -> {
            TimelineGenerator.RowContent selectedToot = tableView.getSelectionModel().getSelectedItem();
            String hostname = selectedToot.dataOriginInfo.hostname;
            String statusId = selectedToot.id;

            if( "mastodon".equals(selectedToot.dataOriginInfo.serverType) ) {
                MastodonWriteAPIParser mastodonWriteAPIParser = selectedToot.writeActionApi;
                mastodonWriteAPIParser.reblog(statusId); // TODO: 成功時、TimelineGeneratorの内部状態への反映
            }
        });

        menuItemUserTimeline.setOnAction((ActionEvent t) -> {
            TimelineGenerator.RowContent selectedToot = tableView.getSelectionModel().getSelectedItem();
            rootController.addUserTab(selectedToot.userId, selectedToot.userName, selectedToot.dataOriginInfo.hostname, selectedToot.dataOriginInfo.getToken());
        });

        menuItemReply.setOnAction((ActionEvent t) -> {
            TimelineGenerator.RowContent selectedToot = tableView.getSelectionModel().getSelectedItem();
            String statusId = selectedToot.id;
            String acct = selectedToot.acct;

            // TODO: データ読み込み元ホストに応じてAPI叩く鯖切り替え
            rootController.userReplyInputStart(statusId, acct);
        });

        menuItemStatusURL.setOnAction((ActionEvent t) -> {
            TimelineGenerator.RowContent selectedToot = tableView.getSelectionModel().getSelectedItem();
            BrowserLauncher.launch(selectedToot.url);
        });

        menuItemInfo.setOnAction((ActionEvent t) -> {
            TimelineGenerator.RowContent selectedToot = tableView.getSelectionModel().getSelectedItem();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.OK);
            alert.setTitle("投稿情報");
            alert.getDialogPane().setHeaderText("投稿情報");
            String contentText = "";
            contentText = contentText + "データソース(種類): " + selectedToot.dataOriginInfo.serverType + "\n";
            contentText = contentText + "データソース(ホスト): " + selectedToot.dataOriginInfo.hostname + "\n";
            contentText = contentText + "データソース(ユーザー): " + selectedToot.dataOriginInfo.username + "\n";
            contentText = contentText + "投稿(閲覧注意): " + selectedToot.sensitive + "\n";
            contentText = contentText + "投稿(お気に入り状態): " + selectedToot.favorited + "\n";
            contentText = contentText + "投稿(リブログ状態): " + selectedToot.reblogged + "\n";
            contentText = contentText + "投稿(ID): " + selectedToot.id + "\n";
            contentText = contentText + "投稿(アカウント): " + selectedToot.acct + "\n";
            contentText = contentText + "クライアント(名前): " + selectedToot.applicationName + "\n";
            contentText = contentText + "クライアント(WebSite): " + selectedToot.applicationWebSite + "\n";
            alert.getDialogPane().setContentText(contentText);
            ButtonType button = alert.showAndWait().orElse(ButtonType.OK);
            System.out.println(button.toString());
        });

        contextMenu.getItems().addAll(menuItemFavorite, menuItemReblog, menuItemUserTimeline, menuItemReply, menuItemStatusURL, menuItemInfo);

        tableView.setOnContextMenuRequested((ContextMenuEvent event) -> {
            contextMenu.show(tableView, event.getScreenX(), event.getScreenY());
            event.consume();
        });

        tableView.setOnMouseClicked((event) -> {
            contextMenu.hide();
        });
    }

    public void initialize(java.net.URL url, java.util.ResourceBundle bundle) {

        contextMenuInit();

        final KeyCombination filterWordKey =
                new KeyCodeCombination(KeyCode.ENTER);

        filterWordField.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if(filterWordKey.match(event)) {
                filterWord();
            }
        });

        if(tableView != null) {

            ObservableList<TableColumn<TimelineGenerator.RowContent, ? >> columns = tableView.getColumns();
            for( TableColumn<TimelineGenerator.RowContent, ?>  column : columns ) column.setSortable(false);

            tableView.setRowFactory(new Callback<TableView<TimelineGenerator.RowContent>, TableRow<TimelineGenerator.RowContent>>() {
                @Override
                public TableRow<TimelineGenerator.RowContent> call(TableView<TimelineGenerator.RowContent> tootCellTableView) {
                    TimelineViewController.TootCell tootCell = new TootCell();
                    tootCell.getStyleClass().add("toot-row");
                    return tootCell;
                }
            });
        }
    }


    String toCharacterReference(String str) {
        int len = str.length();
        int[] codePointArray = new int[str.codePointCount(0, len)];

        for (int i = 0, num = 0; i < len; i = str.offsetByCodePoints(i, 1)) {
            codePointArray[num] = str.codePointAt(i);
            num += 1;
        }

        StringBuffer stringBuffer = new StringBuffer();
        for (int value : codePointArray){
            if(value >= 0x10000){
                stringBuffer.append("&#x" + (Integer.toHexString(value)) + ";");
            }else {
                stringBuffer.append(Character.toChars(value));
            }
        }
        return stringBuffer.toString();
    }
}
