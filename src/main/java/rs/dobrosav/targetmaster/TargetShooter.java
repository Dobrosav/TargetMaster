package rs.dobrosav.targetmaster;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.input.MouseButton;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
// import javafx.scene.media.AudioClip;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TargetShooter extends Application {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    private Group root3D;
    private PerspectiveCamera camera;
    private Group sniperModel;

    private Rotate cameraRotateX = new Rotate(0, Rotate.X_AXIS);
    private Rotate cameraRotateY = new Rotate(0, Rotate.Y_AXIS);

    private double mouseX = 0;
    private double mouseY = 0;

    private List<Node> bullets = new ArrayList<>();
    private List<Node> particles = new ArrayList<>();
    private Group target;
    private Text missedText;
    private int score = 0;
    private Text scoreText;
    private double targetSpeed = 2.0;
    private double targetDirection = 1.0;
    private double targetBoundsX = 200;
    private double targetInitialX = 0;

     private Clip fireClip;
     private Pane simpleCrosshair;
     private Group detailedScopeOverlay;
     private boolean isScoped = false;
     private double breathingOffset = 0;
     private boolean breathingIn = true;
     private double scopeRecoilX = 0;
     private double scopeRecoilY = 0;
     private Text ammoCountText;

    @Override
    public void start(Stage stage) {
        root3D = new Group();

        Box floor = new Box(1000, 1, 1000);
        PhongMaterial floorMaterial = new PhongMaterial();
        floorMaterial.setDiffuseMap(createFloorTexture());
        floor.setMaterial(floorMaterial);
        floor.setTranslateY(50);
        root3D.getChildren().add(floor);

        Box skybox = createSkybox();
        root3D.getChildren().add(skybox);


        target = createNewTarget();
        targetInitialX = target.getTranslateX();
        root3D.getChildren().add(target);

        sniperModel = createSniperModel();

        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);

        Group cameraPivot = new Group(camera, sniperModel);
        cameraPivot.setTranslateY(-20);
        cameraPivot.getTransforms().addAll(cameraRotateY, cameraRotateX);
        root3D.getChildren().add(cameraPivot);

        AmbientLight light = new AmbientLight(Color.rgb(100, 100, 100));
        root3D.getChildren().add(light);

        PointLight pointLight = new PointLight(Color.WHITE);
        pointLight.setTranslateY(-200);
        root3D.getChildren().add(pointLight);


        SubScene subScene = new SubScene(root3D, WIDTH, HEIGHT, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);

        missedText = new Text("Missed!");
        missedText.setFont(new Font("Arial", 48));
        missedText.setFill(Color.RED);
        missedText.setX(WIDTH / 2.0 - missedText.getLayoutBounds().getWidth() / 2);
        missedText.setY(100);
        missedText.setVisible(false);

        scoreText = new Text("Score: 0");
        scoreText.setFont(new Font("Arial", 24));
        scoreText.setFill(Color.WHITE);
        scoreText.setX(20);
        scoreText.setY(40);

         simpleCrosshair = createCustomCrosshair();
         detailedScopeOverlay = createDetailedScopeOverlay();
         detailedScopeOverlay.setVisible(false); // Vidljivo samo pri zoom-u

         Pane mainPane = new Pane(subScene, missedText, scoreText, simpleCrosshair, detailedScopeOverlay);

        Scene scene = new Scene(mainPane, WIDTH, HEIGHT);
        scene.setCursor(Cursor.NONE);

        setupMouseControl(scene);

        stage.setTitle("3D Target Master");
        stage.setScene(scene);
        stage.show();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
            }
        };
        timer.start();

        // Load sounds
        try {
            URL url = getClass().getResource("/sound/shot-and-reload-6158.mp3");
            if (url != null) {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
                AudioFormat baseFormat = audioIn.getFormat();
                AudioFormat decodedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false
                );
                AudioInputStream decodedAudioIn = AudioSystem.getAudioInputStream(decodedFormat, audioIn);
                fireClip = AudioSystem.getClip();
                fireClip.open(decodedAudioIn);
            } else {
                System.err.println("Sound file not found!");
            }
//            hitSound = new AudioClip(getClass().getResource("/sounds/hit.wav").toExternalForm());
        } catch (Exception e) {
            System.err.println("Error loading sounds: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Image createFloorTexture() {
        int width = 256;
        int height = 256;
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();

        Color color1 = Color.web("#8B4513"); // SaddleBrown
        Color color2 = Color.web("#A0522D"); // Sienna

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int tileX = x / 32;
                int tileY = y / 32;
                Color color = (tileX + tileY) % 2 == 0 ? color1 : color2;
                writer.setColor(x, y, color);
            }
        }
        return image;
    }

    private Box createSkybox() {
        Box skybox = new Box(5000, 5000, 5000);
        PhongMaterial skyMaterial = new PhongMaterial();
        skyMaterial.setDiffuseMap(createSkyTexture());
        skybox.setMaterial(skyMaterial);
        skybox.setCullFace(CullFace.NONE);
        return skybox;
    }

    private Image createSkyTexture() {
        int width = 256;
        int height = 256;
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();

        Color topColor = Color.DEEPSKYBLUE;
        Color bottomColor = Color.LIGHTBLUE;

        for (int y = 0; y < height; y++) {
            double ratio = (double) y / height;
            Color interpolatedColor = topColor.interpolate(bottomColor, ratio);
            for (int x = 0; x < width; x++) {
                writer.setColor(x, y, interpolatedColor);
            }
        }
        return image;
    }

    private void setupMouseControl(Scene scene) {
        scene.setOnMouseMoved(event -> {
            double dx = event.getSceneX() - mouseX;
            double dy = event.getSceneY() - mouseY;
            double newAngleX = cameraRotateX.getAngle() - dy * 0.2;
            double newAngleY = cameraRotateY.getAngle() + dx * 0.2;
            if (newAngleX > 45) newAngleX = 45;
            if (newAngleX < -45) newAngleX = -45;
            cameraRotateX.setAngle(newAngleX);
            cameraRotateY.setAngle(newAngleY);
            mouseX = event.getSceneX();
            mouseY = event.getSceneY();
        });

        scene.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                fire();
            } else if (event.getButton() == MouseButton.SECONDARY) {
                toggleScope();
            }
        });
    }

     private Group createSniperModel() {
         Group sniper = new Group();
         PhongMaterial blackMetal = new PhongMaterial(Color.web("#1a1a1a"));
         PhongMaterial gunMetal = new PhongMaterial(Color.web("#444444"));
         PhongMaterial darkGray = new PhongMaterial(Color.web("#333333"));
         PhongMaterial polymerBlack = new PhongMaterial(Color.web("#2a2a2a"));
         PhongMaterial steelGray = new PhongMaterial(Color.web("#505050"));
         
         // MAIN BARREL - Dulji, tanji
         Cylinder barrel = new Cylinder(0.15, 50);
         barrel.setMaterial(gunMetal);
         barrel.setRotationAxis(Rotate.X_AXIS);
         barrel.setRotate(90);
         barrel.setTranslateZ(-20);
         
         // MUZZLE BRAKE - mali sa linijama
         Cylinder muzzleBrake = new Cylinder(0.25, 4);
         muzzleBrake.setMaterial(darkGray);
         muzzleBrake.setRotationAxis(Rotate.X_AXIS);
         muzzleBrake.setRotate(90);
         muzzleBrake.setTranslateZ(-44);
         
         // GAS TUBE - mali cylinder ispod cijevi
         Cylinder gasTube = new Cylinder(0.08, 45);
         gasTube.setMaterial(steelGray);
         gasTube.setRotationAxis(Rotate.X_AXIS);
         gasTube.setRotate(90);
         gasTube.setTranslateZ(-20);
         gasTube.setTranslateY(0.25);
         
         // RECEIVER - manji
         Box receiver = new Box(0.8, 1.5, 8);
         receiver.setMaterial(blackMetal);
         receiver.setTranslateZ(0);
         
         // MAGAZINE - ispod receivera
         Box magazine = new Box(0.5, 0.8, 3);
         magazine.setMaterial(blackMetal);
         magazine.setTranslateZ(-1);
         magazine.setTranslateY(1.1);
         
         // TRIGGER GUARD
         Box triggerGuard = new Box(0.5, 0.8, 2.5);
         triggerGuard.setMaterial(darkGray);
         triggerGuard.setTranslateZ(1);
         triggerGuard.setTranslateY(0.8);
         
         // BOLT HANDLE
         Cylinder boltHandle = new Cylinder(0.08, 3);
         boltHandle.setMaterial(gunMetal);
         boltHandle.setRotationAxis(Rotate.Z_AXIS);
         boltHandle.setRotate(45);
         boltHandle.setTranslateX(0.8);
         boltHandle.setTranslateY(-0.4);
         boltHandle.setTranslateZ(-2);
         
         // SAFETY LEVER - mali detaljčić
         Box safetyLever = new Box(0.15, 0.4, 0.6);
         safetyLever.setMaterial(steelGray);
         safetyLever.setTranslateX(-0.5);
         safetyLever.setTranslateY(0.3);
         safetyLever.setTranslateZ(-3);
         
         // STOCK - manji, jednostavniji
         Box stock = new Box(0.9, 1.5, 12);
         stock.setMaterial(polymerBlack);
         stock.setTranslateZ(10);
         
         // STOCK VENTILATION LINES - vizuelni detalj
         Box ventLeft = new Box(0.2, 1.4, 8);
         ventLeft.setMaterial(darkGray);
         ventLeft.setTranslateZ(8);
         ventLeft.setTranslateX(-0.35);
         ventLeft.setTranslateY(0.8);
         
         Box ventRight = new Box(0.2, 1.4, 8);
         ventRight.setMaterial(darkGray);
         ventRight.setTranslateZ(8);
         ventRight.setTranslateX(0.35);
         ventRight.setTranslateY(0.8);
         
         // GRIP
         Box grip = new Box(0.7, 2.5, 1.5);
         grip.setMaterial(polymerBlack);
         grip.setRotationAxis(Rotate.X_AXIS);
         grip.setRotate(12);
         grip.setTranslateZ(4);
         grip.setTranslateY(2);
         
         // CHEEK REST
         Box cheekRest = new Box(0.8, 0.6, 3);
         cheekRest.setMaterial(polymerBlack);
         cheekRest.setTranslateZ(8);
         cheekRest.setTranslateY(-1.2);
         
         // SCOPE RAIL
         Box scopeRail = new Box(0.3, 0.3, 8);
         scopeRail.setMaterial(darkGray);
         scopeRail.setTranslateY(-1.2);
         scopeRail.setTranslateZ(2);
         
         // SCOPE - manji, minimalistički
         Cylinder scopeTube = new Cylinder(0.35, 10);
         scopeTube.setMaterial(blackMetal);
         scopeTube.setRotationAxis(Rotate.X_AXIS);
         scopeTube.setRotate(90);
         scopeTube.setTranslateY(-1.8);
         scopeTube.setTranslateZ(2);
         
         // SCOPE EYEPIECE - detalj
         Cylinder scopeEye = new Cylinder(0.42, 1.5);
         scopeEye.setMaterial(darkGray);
         scopeEye.setRotationAxis(Rotate.X_AXIS);
         scopeEye.setRotate(90);
         scopeEye.setTranslateY(-1.8);
         scopeEye.setTranslateZ(8);
         
         Cylinder scopeFront = new Cylinder(0.5, 2);
         scopeFront.setMaterial(darkGray);
         scopeFront.setRotationAxis(Rotate.X_AXIS);
         scopeFront.setRotate(90);
         scopeFront.setTranslateY(-1.8);
         scopeFront.setTranslateZ(-3);
         
         Cylinder scopeBack = new Cylinder(0.45, 2);
         scopeBack.setMaterial(darkGray);
         scopeBack.setRotationAxis(Rotate.X_AXIS);
         scopeBack.setRotate(90);
         scopeBack.setTranslateY(-1.8);
         scopeBack.setTranslateZ(6);
         
         // SCOPE RINGS - male kutije za montažu
         Box scopeRingL = new Box(0.25, 0.4, 1);
         scopeRingL.setMaterial(steelGray);
         scopeRingL.setTranslateX(-0.5);
         scopeRingL.setTranslateY(-1.6);
         scopeRingL.setTranslateZ(0);
         
         Box scopeRingR = new Box(0.25, 0.4, 1);
         scopeRingR.setMaterial(steelGray);
         scopeRingR.setTranslateX(0.5);
         scopeRingR.setTranslateY(-1.6);
         scopeRingR.setTranslateZ(0);
         
         // BIPOD - minimalist
         Cylinder bipodL = new Cylinder(0.1, 5);
         bipodL.setMaterial(darkGray);
         bipodL.setTranslateZ(-8);
         bipodL.setTranslateX(-0.9);
         bipodL.setTranslateY(2);
         bipodL.setRotationAxis(Rotate.Z_AXIS);
         bipodL.setRotate(30);
         
         Cylinder bipodR = new Cylinder(0.1, 5);
         bipodR.setMaterial(darkGray);
         bipodR.setTranslateZ(-8);
         bipodR.setTranslateX(0.9);
         bipodR.setTranslateY(2);
         bipodR.setRotationAxis(Rotate.Z_AXIS);
         bipodR.setRotate(-30);
         
         // REAR SIGHT - mali detaljčić
         Box rearSight = new Box(0.15, 0.6, 0.4);
         rearSight.setMaterial(steelGray);
         rearSight.setTranslateZ(4);
         rearSight.setTranslateY(-0.85);
         
         // Assemble
         sniper.getChildren().addAll(
                 barrel, gasTube, muzzleBrake,
                 receiver, magazine, triggerGuard,
                 boltHandle, safetyLever,
                 stock, ventLeft, ventRight, grip, cheekRest,
                 scopeRail, scopeTube, scopeEye, scopeFront, scopeBack, scopeRingL, scopeRingR,
                 bipodL, bipodR, rearSight
         );
         
         sniper.setTranslateZ(10);
         sniper.setTranslateY(4);
         return sniper;
     }

     private void toggleScope() {
         isScoped = !isScoped;
         if (isScoped) {
             detailedScopeOverlay.setVisible(true);
             sniperModel.setVisible(false);
             camera.setFieldOfView(8); // Jaće zoom (5-8 stupnjeva)
         } else {
             detailedScopeOverlay.setVisible(false);
             sniperModel.setVisible(true);
             camera.setFieldOfView(45); // Default FOV
         }
     }

     private Group createScopeOverlay() {
         Group group = new Group();
         double cx = WIDTH / 2;
         double cy = HEIGHT / 2;
         double r = HEIGHT / 2;

         Rectangle screenRect = new Rectangle(0, 0, WIDTH, HEIGHT);
         Circle scopeHole = new Circle(cx, cy, r);

         Shape mask = Shape.subtract(screenRect, scopeHole);
         mask.setFill(Color.BLACK);

         Line hLine = new Line(0, cy, WIDTH, cy);
         hLine.setStroke(Color.BLACK);
         hLine.setStrokeWidth(1);


         Line vLine = new Line(cx, 0, cx, HEIGHT);
         vLine.setStroke(Color.BLACK);
         vLine.setStrokeWidth(1);

         double gap = 50;

         Line thickL = new Line(0, cy, cx - gap, cy);
         thickL.setStroke(Color.BLACK);
         thickL.setStrokeWidth(4);


         Line thickR = new Line(cx + gap, cy, WIDTH, cy);
         thickR.setStroke(Color.BLACK);
         thickR.setStrokeWidth(4);

         Line thickT = new Line(cx, 0, cx, cy - gap);
         thickT.setStroke(Color.BLACK);
         thickT.setStrokeWidth(4);

         Line thickB = new Line(cx, cy + gap, cx, HEIGHT);
         thickB.setStroke(Color.BLACK);
         thickB.setStrokeWidth(4);

         Circle redDot = new Circle(cx, cy, 1.5, Color.RED);

         group.getChildren().addAll(mask, hLine, vLine, thickL, thickR, thickT, thickB, redDot);
         group.setMouseTransparent(true);
         return group;
     }


     private Group createDetailedScopeOverlay() {
         Group group = new Group();
         double cx = WIDTH / 2;
         double cy = HEIGHT / 2;
         double r = Math.min(WIDTH, HEIGHT) / 2.2; // Veći krug za zoom

         // Crna maska oko scope-a
         Rectangle screenRect = new Rectangle(0, 0, WIDTH, HEIGHT);
         Circle scopeHole = new Circle(cx, cy, r);
         Shape mask = Shape.subtract(screenRect, scopeHole);
         mask.setFill(Color.BLACK);

         // Vanjski krug (scope cijev)
         Circle scopeCircle = new Circle(cx, cy, r);
         scopeCircle.setFill(null);
         scopeCircle.setStroke(Color.web("#333333"));
         scopeCircle.setStrokeWidth(4);

         // Unutrašnji krug - detaljniji
         Circle innerCircle = new Circle(cx, cy, r - 8);
         innerCircle.setFill(null);
         innerCircle.setStroke(Color.web("#555555"));
         innerCircle.setStrokeWidth(1);

         // Horizontalna linija (central line)
         Line hLine = new Line(cx - r + 20, cy, cx + r - 20, cy);
         hLine.setStroke(Color.web("#888888"));
         hLine.setStrokeWidth(1);

         // Vertikalna linija (central line)
         Line vLine = new Line(cx, cy - r + 20, cx, cy + r - 20);
         vLine.setStroke(Color.web("#888888"));
         vLine.setStrokeWidth(1);

         // Deblје reticle linije
         double gap = 50;
         
         Line thickL = new Line(cx - r + 20, cy, cx - gap, cy);
         thickL.setStroke(Color.web("#666666"));
         thickL.setStrokeWidth(3);

         Line thickR = new Line(cx + gap, cy, cx + r - 20, cy);
         thickR.setStroke(Color.web("#666666"));
         thickR.setStrokeWidth(3);

         Line thickT = new Line(cx, cy - r + 20, cx, cy - gap);
         thickT.setStroke(Color.web("#666666"));
         thickT.setStrokeWidth(3);

         Line thickB = new Line(cx, cy + gap, cx, cy + r - 20);
         thickB.setStroke(Color.web("#666666"));
         thickB.setStrokeWidth(3);

         // Centralna crna tačka
         Circle centerDot = new Circle(cx, cy, 2.5);
         centerDot.setFill(Color.web("#222222"));

         // Grid linije (fina mreža)
         Group gridLines = new Group();
         int gridSpacing = 40;
         
         // Vertikalne grid linije
         for (int i = (int)(cx - r + 40); i < cx + r - 40; i += gridSpacing) {
             Line gridLine = new Line(i, cy - r + 30, i, cy + r - 30);
             gridLine.setStroke(Color.web("#444444"));
             gridLine.setStrokeWidth(0.5);
             gridLine.setOpacity(0.3);
             gridLines.getChildren().add(gridLine);
         }
         
         // Horizontalne grid linije
         for (int i = (int)(cy - r + 40); i < cy + r - 40; i += gridSpacing) {
             Line gridLine = new Line(cx - r + 30, i, cx + r - 30, i);
             gridLine.setStroke(Color.web("#444444"));
             gridLine.setStrokeWidth(0.5);
             gridLine.setOpacity(0.3);
             gridLines.getChildren().add(gridLine);
         }

         // Markeri za daljinu (distance markers)
         double markerRadius = r - 40;
         
         // Markeri na desnoj strani (100m, 200m, 300m, 400m)
         createDistanceMarker(group, cx + markerRadius - 15, cy, "100");
         createDistanceMarker(group, cx + markerRadius - 35, cy, "200");
         createDistanceMarker(group, cx + markerRadius - 55, cy, "300");
         createDistanceMarker(group, cx + markerRadius - 75, cy, "400");
         
         // Markeri na lijevoj strani (kao mirror)
         createDistanceMarker(group, cx - markerRadius + 15, cy, "100");
         createDistanceMarker(group, cx - markerRadius + 35, cy, "200");
         
         // Markeri na vrhu i dnu
         createDistanceMarker(group, cx, cy - markerRadius + 20, "100");
         createDistanceMarker(group, cx, cy + markerRadius - 20, "100");

         // Wind indicator na vrhu
         createWindIndicator(group, cx, cy - r + 30);

         // Breathing indicator na dnu
         Text breathingText = new Text(cx - 20, cy + r - 15, "BREATHING");
         breathingText.setFont(new Font("Arial", 10));
         breathingText.setFill(Color.web("#666666"));
         breathingText.setOpacity(0.6);

         // Ready indicator
         Text readyText = new Text(cx - 15, cy - r + 25, "READY");
         readyText.setFont(new Font("Arial", 12));
         readyText.setFill(Color.web("#44dd44"));
         readyText.setOpacity(0.7);

         // Ammo counter (desno, dolje)
         ammoCountText = new Text(cx + r - 60, cy + r - 20, "20 / 5");
         ammoCountText.setFont(new Font("Arial", 11));
         ammoCountText.setFill(Color.web("#777777"));
         ammoCountText.setOpacity(0.6);

         // Zoom level indikator (lijevo, dolje)
         Text zoomText = new Text(cx - r + 15, cy + r - 20, "8x ZOOM");
         zoomText.setFont(new Font("Arial", 10));
         zoomText.setFill(Color.web("#555555"));
         zoomText.setOpacity(0.5);

         // Stability indicator (sve linije sa stranama menjaće boju na osnovu breathing-a)
         // Za sada ćemo pokazati "STEADY" kada je breathing stable
         Text stabilityText = new Text(cx - 30, cy - 10, "STEADY");
         stabilityText.setFont(new Font("Arial", 14));
         stabilityText.setFill(Color.web("#44aa44"));
         stabilityText.setOpacity(0.4); // Lagano vidljivo

         group.getChildren().addAll(mask, scopeCircle, innerCircle, gridLines, 
                                      hLine, vLine, thickL, thickR, thickT, thickB, 
                                      centerDot, breathingText, readyText, ammoCountText, zoomText, stabilityText);
         group.setMouseTransparent(true);
         return group;
     }

     private void createDistanceMarker(Group group, double x, double y, String distance) {
         // Mali krugovi kao markeri
         Circle marker = new Circle(x, y, 2.5);
         marker.setFill(null);
         marker.setStroke(Color.web("#555555"));
         marker.setStrokeWidth(1);

         // Kratke linije uz markere (horizontalno)
         Line line1 = new Line(x - 8, y, x - 3, y);
         line1.setStroke(Color.web("#555555"));
         line1.setStrokeWidth(1);
         
         Line line2 = new Line(x + 3, y, x + 8, y);
         line2.setStroke(Color.web("#555555"));
         line2.setStrokeWidth(1);

         // Distanca tekst
         Text distanceLabel = new Text(x + 12, y + 4, distance);
         distanceLabel.setFont(new Font("Arial", 7));
         distanceLabel.setFill(Color.web("#555555"));
         distanceLabel.setOpacity(0.6);

         group.getChildren().addAll(marker, line1, line2, distanceLabel);
     }

     private void createWindIndicator(Group group, double x, double y) {
         // Vjetar indikator - linije sa zagradama
         Text windLabel = new Text(x - 20, y, "WIND:");
         windLabel.setFont(new Font("Arial", 9));
         windLabel.setFill(Color.web("#555555"));
         windLabel.setOpacity(0.6);

         // Tri pozicije za vjetar (lijevo, sredina, desno)
         Line windL = new Line(x + 15, y - 3, x + 15, y + 3);
         windL.setStroke(Color.web("#444444"));
         windL.setStrokeWidth(1);

         Line windM = new Line(x + 25, y - 4, x + 25, y + 4);
         windM.setStroke(Color.web("#555555"));
         windM.setStrokeWidth(2);

         Line windR = new Line(x + 35, y - 3, x + 35, y + 3);
         windR.setStroke(Color.web("#444444"));
         windR.setStrokeWidth(1);

         group.getChildren().addAll(windLabel, windL, windM, windR);
     }

    private Pane createCustomCrosshair() {
        Pane pane = new Pane();
        pane.setMouseTransparent(true);
        Line lineH = new Line(WIDTH / 2 - 10, HEIGHT / 2, WIDTH / 2 + 10, HEIGHT / 2);
        lineH.setStroke(Color.RED);
        lineH.setStrokeWidth(1.5);
        Line lineV = new Line(WIDTH / 2, HEIGHT / 2 - 10, WIDTH / 2, HEIGHT / 2 + 10);
        lineV.setStroke(Color.RED);
        lineV.setStrokeWidth(1.5);
        pane.getChildren().addAll(lineH, lineV);
        return pane;
    }

    private void createMuzzleFlash() {
        Group cameraPivot = (Group) camera.getParent();
        Point3D flashPosition = cameraPivot.localToScene(new Point3D(0, 2.5, 25));

        Group flashGroup = new Group();
        flashGroup.setTranslateX(flashPosition.getX());
        flashGroup.setTranslateY(flashPosition.getY());
        flashGroup.setTranslateZ(flashPosition.getZ());

        Sphere core = new Sphere(1.5);
        core.setMaterial(new PhongMaterial(Color.LIGHTYELLOW));

        Sphere outer = new Sphere(4.0);
        outer.setMaterial(new PhongMaterial(Color.ORANGERED));

        PointLight light = new PointLight(Color.ORANGE);
        light.setMaxRange(150);

        flashGroup.getChildren().addAll(outer, core, light);
        root3D.getChildren().add(flashGroup);

        ScaleTransition st = new ScaleTransition(Duration.millis(50), flashGroup);
        st.setFromX(0.1);
        st.setFromY(0.1);
        st.setFromZ(0.1);
        st.setToX(1.0);
        st.setToY(1.0);
        st.setToZ(1.0);

        FadeTransition ft = new FadeTransition(Duration.millis(100), flashGroup);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);

        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.setOnFinished(e -> root3D.getChildren().remove(flashGroup));
        pt.play();
    }

     private void fire() {
         if (fireClip != null) {
             if (fireClip.isRunning()) {
                 fireClip.stop(); // Stop if already playing
             }
             fireClip.setFramePosition(0); // Rewind
             fireClip.start(); // Play
         }
         Group cameraPivot = (Group) camera.getParent();
         createMuzzleFlash();

         // Bullet
         Sphere bullet = new Sphere(0.5);
         bullet.setMaterial(new PhongMaterial(Color.BLACK));

         Point3D origin = camera.localToScene(0, 0, 0);
         Point3D target = camera.localToScene(0, 0, 1);
         Point3D direction = target.subtract(origin).normalize();

         bullet.setTranslateX(origin.getX());
         bullet.setTranslateY(origin.getY());
         bullet.setTranslateZ(origin.getZ());
         bullet.setUserData(direction);
         bullets.add(bullet);
         root3D.getChildren().add(bullet);

         // Recoil Animation - jaći ako je zoomed
         double recoilAmount = isScoped ? 3.0 : 2.0; // Veći recoil pri zoom-u
         Rotate recoilRotate = new Rotate(-recoilAmount, Rotate.X_AXIS); // Kickback rotation
         cameraPivot.getTransforms().add(recoilRotate);

         // Scope recoil vizuelni efekt
         if (isScoped) {
             scopeRecoilX = -5;
             scopeRecoilY = -3;
         }

         PauseTransition recoilEnd = new PauseTransition(Duration.millis(60));
         recoilEnd.setOnFinished(e -> {
             cameraPivot.getTransforms().remove(recoilRotate);
             scopeRecoilX = 0;
             scopeRecoilY = 0;
         });
         recoilEnd.play();
     }

    private void playHitEffect(Point3D position) {
        List<Sphere> newParticles = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Sphere particle = new Sphere(0.5);
            particle.setMaterial(new PhongMaterial(Color.ORANGERED));
            particle.setTranslateX(position.getX());
            particle.setTranslateY(position.getY());
            particle.setTranslateZ(position.getZ());

            Point3D velocity = new Point3D(
                    (Math.random() - 0.5) * 5,
                    (Math.random() - 0.5) * 5,
                    (Math.random() - 0.5) * 5
            );
            particle.setUserData(velocity);
            newParticles.add(particle);
            particles.add(particle);
            root3D.getChildren().add(particle);
        }

        PauseTransition removeParticles = new PauseTransition(Duration.seconds(1));
        removeParticles.setOnFinished(e -> {
            root3D.getChildren().removeAll(newParticles);
            particles.removeAll(newParticles);
        });
        removeParticles.play();
    }

    private Group createNewTarget() {
        Group targetGroup = new Group();

        Cylinder whitePart = new Cylinder(15, 2.5);
        whitePart.setMaterial(new PhongMaterial(Color.WHITE));

        Cylinder bluePart = new Cylinder(10, 2.6);
        bluePart.setMaterial(new PhongMaterial(Color.BLUE));

        Cylinder redPart = new Cylinder(5, 2.7);
        redPart.setMaterial(new PhongMaterial(Color.RED));

        targetGroup.getChildren().addAll(whitePart, bluePart, redPart);
        targetGroup.setRotationAxis(Rotate.X_AXIS);
        targetGroup.setRotate(90);

        double x = (Math.random() - 0.5) * 400;
        double y = (Math.random() * -50) - 10;
        double z = -(Math.random() * 500 + 400);
        targetGroup.setTranslateX(x);
        targetGroup.setTranslateY(y);
        targetGroup.setTranslateZ(z);
        return targetGroup;
    }

     private void update() {
         // Breathing efekt kada je scoped
         if (isScoped) {
             if (breathingIn) {
                 breathingOffset += 0.01;
                 if (breathingOffset >= 2) {
                     breathingIn = false;
                 }
             } else {
                 breathingOffset -= 0.01;
                 if (breathingOffset <= 0) {
                     breathingIn = true;
                 }
             }
             
             // Primijeni breathing efekt na scope - mali zoom
             double breathingScale = 1.0 + (breathingOffset - 1) * 0.02;
             detailedScopeOverlay.setScaleX(breathingScale);
             detailedScopeOverlay.setScaleY(breathingScale);
             
             // Reset na centar
             double offsetX = (breathingScale - 1) * (-WIDTH / 2) + scopeRecoilX;
             double offsetY = (breathingScale - 1) * (-HEIGHT / 2) + scopeRecoilY;
             detailedScopeOverlay.setTranslateX(offsetX);
             detailedScopeOverlay.setTranslateY(offsetY);
             
             // Lagano smanjivanje recoil offset-a
             scopeRecoilX *= 0.92;
             scopeRecoilY *= 0.92;
         } else {
             // Resetuj breathing efekt kada nije zoomed
             detailedScopeOverlay.setScaleX(1.0);
             detailedScopeOverlay.setScaleY(1.0);
             detailedScopeOverlay.setTranslateX(0);
             detailedScopeOverlay.setTranslateY(0);
             scopeRecoilX = 0;
             scopeRecoilY = 0;
         }

         for (Node bullet : new ArrayList<>(bullets)) {
            Point3D direction = (Point3D) bullet.getUserData();
            Point3D prevPos = new Point3D(bullet.getTranslateX(), bullet.getTranslateY(), bullet.getTranslateZ());

            bullet.setTranslateX(bullet.getTranslateX() + direction.getX() * 20);
            bullet.setTranslateY(bullet.getTranslateY() + direction.getY() * 20);
            bullet.setTranslateZ(bullet.getTranslateZ() + direction.getZ() * 20);

            Point3D currPos = new Point3D(bullet.getTranslateX(), bullet.getTranslateY(), bullet.getTranslateZ());
            Point3D targetPos = new Point3D(target.getTranslateX(), target.getTranslateY(), target.getTranslateZ());

            Point3D V = currPos.subtract(prevPos);
            Point3D L = targetPos.subtract(prevPos);

            double t = L.dotProduct(V) / V.dotProduct(V);
            t = Math.max(0, Math.min(1, t));

            Point3D closestPoint = prevPos.add(V.multiply(t));
            double distance = closestPoint.distance(targetPos);

            if (distance < 15) { // 15 is the radius of the largest target cylinder
                System.out.println("Target Hit!");
                score++;
                scoreText.setText("Score: " + score);
                root3D.getChildren().remove(target);
                target = createNewTarget();
                targetInitialX = target.getTranslateX();
                root3D.getChildren().add(target);
                root3D.getChildren().remove(bullet);
                bullets.remove(bullet);
            } else if (Math.abs(bullet.getTranslateZ()) > 1000 || Math.abs(bullet.getTranslateX()) > 1000) {
                root3D.getChildren().remove(bullet);
                bullets.remove(bullet);
                if (!missedText.isVisible()) {
                    missedText.setVisible(true);
                    PauseTransition missVanish = new PauseTransition(Duration.seconds(2));
                    missVanish.setOnFinished(e -> missedText.setVisible(false));
                    missVanish.play();
                }
            }
        }

        for (Node particle : particles) {
            Point3D velocity = (Point3D) particle.getUserData();
            particle.setTranslateX(particle.getTranslateX() + velocity.getX());
            particle.setTranslateY(particle.getTranslateY() + velocity.getY());
            particle.setTranslateZ(particle.getTranslateZ() + velocity.getZ());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
