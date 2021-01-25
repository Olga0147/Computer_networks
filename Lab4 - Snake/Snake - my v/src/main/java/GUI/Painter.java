package GUI;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.canvas.Canvas;
import javafx.stage.StageStyle;

import java.io.InputStream;

public class Painter extends Application {

    int stageWidth = 800;
    int stageHeight = 450;
    int spacing = 3;

   /* int numRows = 30;
    int numColumns = 40;

    void setRowsColumns(int r,int c){
        this.numRows=r;
        this.numColumns=c;
    }
*/
    public void start(Stage primaryStage) throws Exception {

        //------------------------------------------------------------window setting
        primaryStage.setTitle("Snake");
        InputStream iconStream = getClass().getResourceAsStream("/icon.png");
        Image image = new Image(iconStream);
        primaryStage.getIcons().add(image);
        primaryStage.setResizable(false);
        primaryStage.setHeight(stageHeight);
        primaryStage.setWidth(stageWidth);

        //------------------------------------------------------------

        GameScene gameScene = new GameScene();
        NewGameSettingsScene newGameSettingsScene = new NewGameSettingsScene();
        PrimaryScene primaryScene = new PrimaryScene(primaryStage,newGameSettingsScene.getNewGameSettingsScene());

        primaryStage.setScene(primaryScene.getPrimaryScene());
        primaryStage.show();
    }


    private class PrimaryScene{

        VBox vbox;
        Button newGame;
        Button joinGame;
        Scene primaryScene;
        PrimaryScene(Stage primaryStage, Scene newGameSettingsScene){
            this.vbox = new VBox(spacing);
            this.vbox.setAlignment(Pos.CENTER);
            this.vbox.setBackground(new Background(new BackgroundFill(Color.valueOf("#C9F76F"), CornerRadii.EMPTY, Insets.EMPTY)));
            this.newGame = new Button("New game");
            this.joinGame = new Button("Join exist game");
            this.newGame.setStyle("-fx-border-color: #ff0000; -fx-border-width: 5px;");
            this.joinGame.setStyle("-fx-border-color: #ff0000; -fx-border-width: 5px;");

            this.newGame.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    primaryStage.close();
                    primaryStage.setScene(newGameSettingsScene);
                    primaryStage.show();
                }
            });
            this.joinGame.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    //primaryStage.setScene(gameScene);
                }
            });
            this.vbox.getChildren().addAll(newGame, joinGame);
            this.primaryScene = new  Scene(vbox);
        }

        public Scene getPrimaryScene() {
            return primaryScene;
        }
    }

    private class NewGameSettingsScene{
        VBox settings;
        HBox playerName;
        Label nameL;
        TextField nameTF;
        HBox  sizeW;
        Label wL;
        TextField wTF;
        HBox  sizeH;
        Label hL;
        TextField hTF;
        Button set;
        Scene newGameSettingsScene;

        NewGameSettingsScene(){
            this.settings = new VBox(spacing);
            this.settings.setAlignment(Pos.CENTER);
            this.settings.setBackground(new Background(new BackgroundFill(Color.valueOf("#C9F76F"),CornerRadii.EMPTY,Insets.EMPTY)) );

            this.playerName = new HBox(spacing);
            this.nameL = new Label("Name ");
            this.nameTF = new TextField();
            this.playerName.getChildren().addAll(nameL,nameTF);
            this.playerName.setAlignment(Pos.CENTER);

            this.sizeW = new HBox(spacing);
            this.wL = new Label("Field width in cells (from 10 to 100) ");
            this.wTF = new TextField();
            this.sizeW.getChildren().addAll(wL,wTF);
            this.sizeW.setAlignment(Pos.CENTER);

            this.sizeH = new HBox(spacing);
            this.hL = new Label("Field height in cells (from 10 to 100) ");
            this.hTF = new TextField();
            this.sizeH.getChildren().addAll(hL,hTF);
            this.sizeH.setAlignment(Pos.CENTER);

            this.set = new Button("OK");
            this.set.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    //check
                    //что-то

                }
            });

            this.settings.getChildren().addAll(playerName,sizeH,sizeW,set);

            this.newGameSettingsScene  = new Scene(settings);
        }

        public Scene getNewGameSettingsScene() {
            return newGameSettingsScene;
        }
    }

    private class GameScene{
        GridPane gridPane;
        Canvas canvas;
        GraphicsContext gc;
        VBox rating;
        Scene gameScene;
        int weight;
        int height;

        GameScene(){
        weight=400;
        height=400;

            this.gridPane = new GridPane();
            this.gridPane.setHgap(spacing*2); //Ширина горизонтальных промежутков между столбцами.
            this.gameScene =  new Scene(gridPane,stageWidth,stageWidth);

            this.canvas = new Canvas(weight,height);
            this.gc = canvas.getGraphicsContext2D();

            this.rating = new VBox();
            rating.setBackground(new Background(new BackgroundFill(Color.valueOf("#C9F76F"), CornerRadii.EMPTY, Insets.EMPTY)));
            addRatingLabel("1: ","A ","4");

            gridPane.add(canvas,0,0);
            gridPane.add(rating,1,0);

            gc.setFill(Color.LIGHTBLUE);
            gc.fillRect(0, 0,  this.weight,this.height);



        }

        public Scene getGameScene() {
            return gameScene;
        }

        void addRatingLabel(String rat,String name,String score){
            HBox participant = new HBox();
            participant.getChildren().addAll(new Label(rat),new Label(name), new Label(score));
            rating.getChildren().add(participant);
        }
    }

}
