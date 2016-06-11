package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.ThreadLocalRandom;

/**
 * test
 *
 * @author normenhansen
 */
public class Main extends SimpleApplication implements ActionListener {

    BetterCharacterControl player;
    Node playerNode = new Node("playerNode");
    BulletAppState bullet = new BulletAppState();
    int worldSize = 32;
    double[][] HeightMap = new double[worldSize + 1][worldSize + 1];
    boolean leftPressed;
    boolean rightPressed;
    boolean forwardPressed;
    boolean backPressed;
    Random random = new Random();
    private Cell currentCell;
    private int[][] maze = new int[worldSize][worldSize];
    private Cell chosenCell = new Cell(0, 0);
    Stack<Cell> stack = new Stack<Cell>();
    Texture2D inventoryBackgroundTexture;
    Texture2D inventoryBackgroundActiveTexture;
    String[] textureNameArray = {"Textures/Gras.png", "Textures/Wood.png", "Textures/Dirt.png", "Textures/Leaves.png", "Textures/Stone.png", "Textures/Window.png", "Textures/Brick.png", "Textures/Water.png", "Textures/Ground.png"};
    Texture2D[] inventoryTexture = new Texture2D[textureNameArray.length];
    Texture[] textureArray = new Texture[textureNameArray.length];
    Texture[] worldtexture = new Texture[worldSize * worldSize * worldSize];
    int[] textureNumber = new int[textureNameArray.length];
    int inventorySize = textureNameArray.length;
    int activeInventorySlot = 0;

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    @SuppressWarnings("empty-statement")
    public void simpleInitApp() {
        for (int i = 0; i < textureNameArray.length; i++) {
            textureArray[i] = assetManager.loadTexture(textureNameArray[i]);
            textureArray[i].setMagFilter(Texture.MagFilter.Nearest);
            textureNumber[i] = 500;
        }
        inventoryBackgroundTexture = new Texture2D(assetManager.loadTexture("Textures/inventoryBackground.png").getImage());
        inventoryBackgroundTexture.setMagFilter(Texture.MagFilter.Nearest);
        inventoryBackgroundActiveTexture = new Texture2D(assetManager.loadTexture("Textures/inventoryBackgroundActive.png").getImage());
        inventoryBackgroundActiveTexture.setMagFilter(Texture.MagFilter.Nearest);
        for (int k = 0; k < inventorySize; k++) {
            Picture pictureB = new Picture("IventoryBackground" + k);
            pictureB.setTexture(assetManager, inventoryBackgroundTexture, false);
            pictureB.setHeight(80);
            pictureB.setWidth(80);
            pictureB.setPosition(((1366 / 2) - inventorySize * 40) + k * 80, 50);
            guiNode.attachChild(pictureB);

            inventoryTexture[k] = new Texture2D(textureArray[k].getImage());
            inventoryTexture[k].setMagFilter(Texture.MagFilter.Nearest);

            Picture pictureC = new Picture("IventoryItem" + k);
            pictureC.setTexture(assetManager, inventoryTexture[k], false);
            pictureC.setHeight(76);
            pictureC.setWidth(76);
            pictureC.setPosition(((1370 / 2) - inventorySize * 40) + k * 80, 52);
            guiNode.attachChild(pictureC);
        }
        ((Picture) guiNode.getChild("IventoryBackground"
                + activeInventorySlot)).setTexture(assetManager, inventoryBackgroundActiveTexture, false);

        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_NUMPAD4));
        inputManager.addListener(this, "Left");
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_NUMPAD6));
        inputManager.addListener(this, "Right");
        inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_NUMPAD8));
        inputManager.addListener(this, "Forward");
        inputManager.addMapping("Back", new KeyTrigger(KeyInput.KEY_NUMPAD2));
        inputManager.addListener(this, "Back");
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_NUMPAD5));
        inputManager.addListener(this, "Jump");

        inputManager.addMapping("RemoveBlock", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "RemoveBlock");
        inputManager.addMapping("AddBlock", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(this, "AddBlock");

        inputManager.addMapping("Save", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addListener(this, "Save");
        inputManager.addMapping("Load", new KeyTrigger(KeyInput.KEY_L));
        inputManager.addListener(this, "Load");

        inputManager.addMapping("Throw", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, "Throw");

        inputManager.addMapping("LeftArrow", new KeyTrigger(KeyInput.KEY_F4));
        inputManager.addListener(this, "LeftArrow");
        inputManager.addMapping("RightArrow", new KeyTrigger(KeyInput.KEY_F6));
        inputManager.addListener(this, "RightArrow");

        inputManager.addMapping("Inventory", new KeyTrigger(KeyInput.KEY_E));
        inputManager.addListener(this, "Inventory");

        stateManager.attach(bullet);
        player = new BetterCharacterControl(1, 4, 1);
        player.setJumpForce(new Vector3f(0, 7, 0));
        playerNode.setLocalTranslation(1, 30, 1);
        playerNode.addControl(player);
        bullet.getPhysicsSpace().add(player);
        rootNode.attachChild(playerNode);
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+");
        ch.setLocalTranslation(settings.getWidth() / 2 - ch.getLineWidth() / 2,
                settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
        currentCell = new Cell(0, 0);
        GenerateMaze(currentCell);
        getHeightMap();
        MakeWorld();
    }

    @SuppressWarnings("empty-statement")
    private void MakeWorld() {
        getHeightMap();
        for (int i = 0; i < worldSize; i++) {
            for (int j = 0; j < worldSize; j++) {
                for (int Height = 0; Height <= HeightMap[j][i]; Height++) {
                    if (Height == Math.floor(HeightMap[j][i])) {
                        setBox(textureArray[0], 1, 1, 1, i * 2, (Height + 1) * 2, j * 2);
                    } else {
                        setBox(textureArray[8], 1, 1, 1, i * 2, (Height + 1) * 2, j * 2);
                    }
                }
            }
        }
    }

    private void getHeightMap() {
        Random rnd = new Random();
        HeightMap[0][0] = 6 /*+ rnd.nextInt(10)*/;
        HeightMap[worldSize - 1][0] = 6 /*+ rnd.nextInt(10)*/;
        HeightMap[0][worldSize - 1] = 6 /*+ rnd.nextInt(10)*/;
        HeightMap[worldSize - 1][worldSize - 1] = 6 /*+ rnd.nextInt(10)*/;
        int ex = (int) Math.round(0 + (worldSize - 1) / 2.0);
        int ey = (int) Math.round(0 + (worldSize - 1) / 2.0);
        int level = 0;
        insert(0, 0, worldSize - 1, 0, 0, worldSize - 1, worldSize - 1, worldSize - 1, rnd, level);
        for (int i = 0; i < worldSize; i++) {
            for (int j = 0; j < worldSize; j++) {
                System.out.format("%1.3f ", HeightMap[i][j]);
            }
            System.out.println();
        }

    }

    private void insert(int ax, int ay, int bx, int by, int cx, int cy, int dx, int dy, Random rnd, int level) {
        if (Math.abs(dx - ax) > 1 || Math.abs(dy - ay) > 1) {
            int ex = (int) Math.round(ax + (dx - ax) / 2.0);
            int ey = (int) Math.round(ay + (dy - ay) / 2.0);
            if (level < 2) {
                HeightMap[ex][ey] = mittelWert(new float[]{(float) HeightMap[ax][ay], (float) HeightMap[bx][by], (float) HeightMap[cy][cy], (float) HeightMap[dx][dy]}, rnd) + (rnd.nextInt(8) - 4);
                HeightMap[ax][ey] = mittelWert(new float[]{(float) HeightMap[ax][ay], (float) HeightMap[cx][cy]}, rnd) + (rnd.nextInt(8) - 4);
                HeightMap[ex][ay] = mittelWert(new float[]{(float) HeightMap[ax][ay], (float) HeightMap[bx][by]}, rnd) + (rnd.nextInt(8) - 4);
                HeightMap[bx][ey] = mittelWert(new float[]{(float) HeightMap[bx][by], (float) HeightMap[dx][dy]}, rnd) + (rnd.nextInt(8) - 4);
                HeightMap[ex][dy] = mittelWert(new float[]{(float) HeightMap[cx][cy], (float) HeightMap[dx][dy]}, rnd) + (rnd.nextInt(8) - 4);

            } else {
                HeightMap[ex][ey] = mittelWert(new float[]{(float) HeightMap[ax][ay], (float) HeightMap[bx][by], (float) HeightMap[cy][cy], (float) HeightMap[dx][dy]}, rnd);
                HeightMap[ax][ey] = mittelWert(new float[]{(float) HeightMap[ax][ay], (float) HeightMap[cx][cy]}, rnd);
                HeightMap[ex][ay] = mittelWert(new float[]{(float) HeightMap[ax][ay], (float) HeightMap[bx][by]}, rnd);
                HeightMap[bx][ey] = mittelWert(new float[]{(float) HeightMap[bx][by], (float) HeightMap[dx][dy]}, rnd);
                HeightMap[ex][dy] = mittelWert(new float[]{(float) HeightMap[cx][cy], (float) HeightMap[dx][dy]}, rnd);

            }
            level++;
            insert(ax, ay, ex, ay, ax, ey, ex, ey, rnd, level);
            insert(ex, ay, bx, ay, ex, ey, bx, ey, rnd, level);
            insert(cx, ey, ex, ey, cx, cy, ex, cy, rnd, level);
            insert(ex, ey, dx, ey, ex, dy, dx, dy, rnd, level);
            level--;
        }
    }

    private double mittelWert(float[] numbers, Random rnd) {
        double sum = 0;
        for (int i = 0; i < numbers.length; i++) {
            sum += numbers[i];
        }
        return (sum / numbers.length);
    }

    private void setBox(Texture texture, float width, float height, float front, float x, float y, float z) {
        Box b = new Box(width, height, front);
        Geometry geom = new Geometry("Box", b);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", texture);
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        geom.setQueueBucket(Bucket.Transparent);
        geom.setMaterial(mat);
        geom.setLocalTranslation(x, y, z);
        rootNode.attachChild(geom);
        worldtexture[rootNode.getChildren().size() - 1] = texture;

        RigidBodyControl boxBody = new RigidBodyControl(0);
        geom.addControl(boxBody);
        bullet.getPhysicsSpace().add(boxBody);
    }

    private void Load() {
        try {
            List<String> input = Files.readAllLines(Paths.get("world.bbb"), Charset.defaultCharset());
            while (rootNode.getChildren().size() > 0) {
                Spatial s = rootNode.getChildren().get(0);
                bullet.getPhysicsSpace().remove(s);
                s.removeFromParent();
            }

            rootNode.attachChild(playerNode);
            String[] dataPlayer = input.get(0).split(" ");
            float xPlayer = Float.valueOf(dataPlayer[0]);
            float yPlayer = Float.valueOf(dataPlayer[1]);
            float zPlayer = Float.valueOf(dataPlayer[2]);
            bullet.getPhysicsSpace().add(player);
            player.warp(new Vector3f(xPlayer, yPlayer, zPlayer));

            for (int i = 1; i < input.size(); i++) {
                String[] data = input.get(i).split(" ");
                float x = Float.valueOf(data[0]);
                float y = Float.valueOf(data[1]);
                float z = Float.valueOf(data[2]);
                Texture t = assetManager.loadTexture(String.valueOf(data[3]));
                t.setMagFilter(Texture.MagFilter.Nearest);
                setBox(t, 1, 1, 1, x, y, z);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void Save() {
        try {
            Vector3f playerPosition = playerNode.getLocalTranslation();
            String positionString = playerPosition.x + " " + playerPosition.y + " " + playerPosition.z + "\r\n";
            for (int i = 0; i < rootNode.getChildren().size(); i++) {
                if (rootNode.getChildren().get(i).getName().equals("Box")) {
                    Vector3f position = rootNode.getChildren().get(i).getLocalTranslation();
                    positionString = positionString + (position.x + " " + position.y + " " + position.z + " " + worldtexture[i].getName() + "\r\n");
                }
            }
            Files.write(Paths.get("world.bbb"), positionString.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code

        Vector3f walkDirection = new Vector3f();
        final Vector3f left = cam.getLeft().mult(5);
        left.setY(0);
        final Vector3f forward = cam.getDirection().mult(5);
        forward.setY(0);
        final Vector3f direction = cam.getDirection().mult(5);
        direction.setY(0);
        if (leftPressed) {
            walkDirection.addLocal(left);
        }
        if (rightPressed) {
            walkDirection.addLocal(left.negate());
        }
        if (forwardPressed) {
            walkDirection.addLocal(forward);
        }
        if (backPressed) {
            walkDirection.addLocal(forward.negate());
        }

        player.setWalkDirection(walkDirection);
        cam.setLocation(playerNode.getWorldTranslation().add(0, 3, 0));
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("Left")) {
            leftPressed = isPressed;
        }
        if (name.equals("Right")) {
            rightPressed = isPressed;
        }
        if (name.equals("Forward")) {
            forwardPressed = isPressed;
        }
        if (name.equals("Back")) {
            backPressed = isPressed;
        }
        if (name.equals("Jump")) {
            player.jump();
        }
        if (name.equals("RemoveBlock") && !isPressed) {
            CollisionResults results = new CollisionResults();
            Ray ray = new Ray(cam.getLocation(), cam.getDirection());
            rootNode.collideWith(ray, results);
            if (results.size() > 0) {
                Geometry g = results.getClosestCollision().getGeometry();
                bullet.getPhysicsSpace().remove(g);
                g.removeFromParent();
            }
        }

        if (name.equals("AddBlock") && !isPressed) {
            addBox();
        }
        if (name.equals("Save") && !isPressed) {
            Save();
        }
        if (name.equals("Load") && !isPressed) {
            Load();
        }
        if (name.equals("Throw") && !isPressed) {
            ThrowSphere();
        }
        if (name.equals("LeftArrow") && !isPressed) {
            LastInventory();
        }
        if (name.equals("RightArrow") && !isPressed) {
            NextInventory();
        }

        if (name.equals("Inventory") && !isPressed) {
            ShowInventory();
        }
    }

    private void addBox() {
        if (textureNumber[activeInventorySlot] > 0) {
            textureNumber[activeInventorySlot] -= 1;
            CollisionResults results = new CollisionResults();
            Ray ray = new Ray(cam.getLocation(), cam.getDirection());
            rootNode.collideWith(ray, results);
            if (results.size() > 0) {
                Vector3f p = results.getClosestCollision().getContactPoint();
                Geometry g = results.getClosestCollision().getGeometry();
                if (g.getLocalTranslation().x == p.x - 1) {
                    setBox(textureArray[activeInventorySlot], 1, 1, 1,
                            g.getLocalTranslation().x + 2,
                            g.getLocalTranslation().y,
                            g.getLocalTranslation().z);

                }
                if (g.getLocalTranslation().x == p.x + 1) {
                    setBox(textureArray[activeInventorySlot], 1, 1, 1,
                            g.getLocalTranslation().x - 2,
                            g.getLocalTranslation().y,
                            g.getLocalTranslation().z);

                }
                if (g.getLocalTranslation().y == p.y - 1) {
                    setBox(textureArray[activeInventorySlot], 1, 1, 1,
                            g.getLocalTranslation().x,
                            g.getLocalTranslation().y + 2,
                            g.getLocalTranslation().z);

                }
                if (g.getLocalTranslation().y == p.y + 1) {
                    setBox(textureArray[activeInventorySlot], 1, 1, 1,
                            g.getLocalTranslation().x,
                            g.getLocalTranslation().y - 2,
                            g.getLocalTranslation().z);

                }
                if (g.getLocalTranslation().z == p.z - 1) {
                    setBox(textureArray[activeInventorySlot], 1, 1, 1,
                            g.getLocalTranslation().x,
                            g.getLocalTranslation().y,
                            g.getLocalTranslation().z + 2);

                }
                if (g.getLocalTranslation().z == p.z + 1) {
                    setBox(textureArray[activeInventorySlot], 1, 1, 1,
                            g.getLocalTranslation().x,
                            g.getLocalTranslation().y,
                            g.getLocalTranslation().z - 2);
                }
            }
        }
    }

    private void LastInventory() {
        ((Picture) guiNode.getChild("IventoryBackground"
                + activeInventorySlot)).setTexture(assetManager, inventoryBackgroundTexture, false);
        activeInventorySlot--;
        if (activeInventorySlot == -1) {
            activeInventorySlot = inventorySize - 1;
        }
        ((Picture) guiNode.getChild("IventoryBackground"
                + activeInventorySlot)).setTexture(assetManager, inventoryBackgroundActiveTexture, false);
    }

    private void NextInventory() {
        ((Picture) guiNode.getChild("IventoryBackground"
                + activeInventorySlot)).setTexture(assetManager, inventoryBackgroundTexture, false);
        activeInventorySlot++;
        if (activeInventorySlot == inventorySize) {
            activeInventorySlot = 0;
        }
        ((Picture) guiNode.getChild("IventoryBackground"
                + activeInventorySlot)).setTexture(assetManager, inventoryBackgroundActiveTexture, false);
    }

    private void ThrowSphere() {
        Sphere s = new Sphere(16, 16, 0.5f);
        Geometry geom = new Geometry("Sphere", s);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        geom.setMaterial(mat);
        geom.setLocalTranslation(cam.getLocation().add(cam.getDirection().mult(2)));
        rootNode.attachChild(geom);
        RigidBodyControl boxBody = new RigidBodyControl(1);
        geom.addControl(boxBody);
        bullet.getPhysicsSpace().add(boxBody);
        boxBody.setLinearVelocity(cam.getDirection().mult(15));
    }

    private void ShowInventory() {
    }

    private void GenerateMaze(Cell currentCell) {
        maze[currentCell.getX()][currentCell.getY()] = 1;
        while (isNotFull()) {
            int x = currentCell.getX();
            int y = currentCell.getY();
            boolean allVisited = true;
            int i = -2;
            int j = 0;
            if ((x + i) >= 0 && (x + i) < worldSize && (y + j) >= 0 && (y + j) < worldSize) {
                if (maze[x + i][y + j] == 0) {
                    allVisited = false;
                }
            }
            i = 2;
            j = 0;
            if ((x + i) >= 0 && (x + i) < worldSize && (y + j) >= 0 && (y + j) < worldSize) {
                if (maze[x + i][y + j] == 0) {
                    allVisited = false;
                }
            }
            i = 0;
            j = -2;
            if ((x + i) >= 0 && (x + i) < worldSize && (y + j) >= 0 && (y + j) < worldSize) {
                if (maze[x + i][y + j] == 0) {
                    allVisited = false;
                }
            }
            i = 0;
            j = 2;
            if ((x + i) >= 0 && (x + i) < worldSize && (y + j) >= 0 && (y + j) < worldSize) {
                if (maze[x + i][y + j] == 0) {
                    allVisited = false;
                }
            }

            if (allVisited == false) {
                boolean found = false;
                while (found == false) {
                    int k = ThreadLocalRandom.current().nextInt(0, 2);
                    int l = ThreadLocalRandom.current().nextInt(0, 2);
                    if (k == 0 && l == 0) {
                        i = -2;
                        j = 0;
                    }
                    if (k == 0 && l == 1) {
                        i = 2;
                        j = 0;
                    }
                    if (k == 1 && l == 0) {
                        i = 0;
                        j = -2;
                    }
                    if (k == 1 && l == 1) {
                        i = 0;
                        j = 2;
                    }
                    if ((x + i) >= 0 && (x + i) < worldSize && (y + j) >= 0 && (y + j) < worldSize) {
                        if (maze[x + i][y + j] == 0) {
                            found = true;
                            chosenCell.setCell(x + i, y + j);
                        }
                    }
                }
                Cell stackCell = new Cell(currentCell.getX(), currentCell.getY());
                stack.push(stackCell);
                if (i == -2 && j == 0) {
                    maze[x - 1][y] = 1;
                }
                if (i == 2 && j == 0) {
                    maze[x + 1][y] = 1;
                }
                if (i == 0 && j == -2) {
                    maze[x][y - 1] = 1;
                }
                if (i == 0 && j == 2) {
                    maze[x][y + 1] = 1;
                }
                maze[chosenCell.getX()][chosenCell.getY()] = 1;
                currentCell = chosenCell;
            } else if (!stack.empty()) {
                currentCell = stack.pop();
            }
        }
    }

    private boolean isNotFull() {
        boolean notFull = false;
        for (int i = 0; i < worldSize; i += 2) {
            for (int j = 0; j < worldSize; j += 2) {
                if (maze[i][j] == 0) {
                    notFull = true;
                }
            }
        }
        return notFull;
    }
}
