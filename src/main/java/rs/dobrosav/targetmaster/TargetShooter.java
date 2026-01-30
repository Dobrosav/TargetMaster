package rs.dobrosav.targetmaster;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
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
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Line;
import javafx.scene.shape.Sphere;
// import javafx.scene.media.AudioClip;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;

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

//    private AudioClip fireSound;
//    private AudioClip hitSound;

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

        Pane crosshairPane = createCustomCrosshair();
        Pane mainPane = new Pane(subScene, missedText, scoreText, crosshairPane);

        Scene scene = new Scene(mainPane, WIDTH, HEIGHT);
        scene.setCursor(Cursor.NONE); // Hide the default cursor

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

//        // Load sounds
//        try {
//            fireSound = new AudioClip(getClass().getResource("/sounds/gunshot.wav").toExternalForm());
//            hitSound = new AudioClip(getClass().getResource("/sounds/hit.wav").toExternalForm());
//        } catch (Exception e) {
//            System.err.println("Error loading sounds: " + e.getMessage());
//            e.printStackTrace();
//        }
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
            }
        });
    }

    private Group createSniperModel() {
        Group sniper = new Group();
        PhongMaterial bodyMaterial = new PhongMaterial(Color.web("#333"));
        PhongMaterial metalMaterial = new PhongMaterial(Color.web("#888"));

        Box body = new Box(2, 3, 18);
        body.setMaterial(bodyMaterial);
        body.setTranslateZ(5);

        Cylinder barrel = new Cylinder(0.5, 12);
        barrel.setMaterial(metalMaterial);
        barrel.setRotationAxis(Rotate.X_AXIS);
        barrel.setRotate(90);
        barrel.setTranslateZ(-7);

        Box stock = new Box(2, 4, 6);
        stock.setMaterial(bodyMaterial);
        stock.setTranslateZ(17);
        stock.setTranslateY(1);

        Cylinder scopeTube = new Cylinder(0.8, 7);
        scopeTube.setMaterial(new PhongMaterial(Color.BLACK));
        scopeTube.setRotationAxis(Rotate.X_AXIS);
        scopeTube.setRotate(90);
        scopeTube.setTranslateY(-2.5);
        scopeTube.setTranslateZ(5);
        
        sniper.getChildren().addAll(body, barrel, stock, scopeTube);
        sniper.setTranslateZ(10);
        sniper.setTranslateY(5);
        return sniper;
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

    private void fire() {
//        if (fireSound != null) {
//            fireSound.play();
//        }
        // Muzzle Flash
        Sphere flash = new Sphere(1.2);
        flash.setMaterial(new PhongMaterial(Color.ORANGE));
        Group cameraPivot = (Group) camera.getParent();
        Point3D flashPosition = cameraPivot.localToScene(new Point3D(0, 0, -14));
        flash.setTranslateX(flashPosition.getX());
        flash.setTranslateY(flashPosition.getY());
        flash.setTranslateZ(flashPosition.getZ());
        root3D.getChildren().add(flash);
        PauseTransition flashVanish = new PauseTransition(Duration.millis(50));
        flashVanish.setOnFinished(e -> root3D.getChildren().remove(flash));
        flashVanish.play();

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

        // Recoil Animation
        Rotate recoilRotate = new Rotate(-2, Rotate.X_AXIS); // Kickback rotation
        cameraPivot.getTransforms().add(recoilRotate);

        PauseTransition recoilEnd = new PauseTransition(Duration.millis(60));
        recoilEnd.setOnFinished(e -> cameraPivot.getTransforms().remove(recoilRotate));
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
//        // Move the target
//        double newX = target.getTranslateX() + targetDirection * targetSpeed;
//        if (Math.abs(newX - targetInitialX) > targetBoundsX) {
//            targetDirection *= -1;
//        }
//        target.setTranslateX(newX);

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
//                if (hitSound != null) {
//                    hitSound.play();
//                }
                playHitEffect(closestPoint);
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
