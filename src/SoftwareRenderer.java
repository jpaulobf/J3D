import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MOTOR 3D NA CPU v7.0 - 10.000 OBJECTS INSANITY TEST
 * 10.000 Objetos Rotacionando + 5 Luzes Dinâmicas.
 */

class Vertex {
    double x, y, z, w = 1.0;
    Vertex(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
}

class Triangle {
    int v1, v2, v3;
    Color baseColor;
    Triangle(int a, int b, int c, Color col) { this.v1 = a; this.v2 = b; this.v3 = c; this.baseColor = col; }
}

class PointLight {
    public Vertex pos;
    public Color color;
    public double intensity;
    PointLight(double x, double y, double z, Color col, double intensity) {
        this.pos = new Vertex(x, y, z); this.color = col; this.intensity = intensity;
    }
}

class Mesh {
    List<Vertex> vertices = new ArrayList<>();
    List<Triangle> triangles = new ArrayList<>();

    public static Mesh createCube() {
        Mesh m = new Mesh();
        for(int x=-1;x<=1;x+=2)for(int y=-1;y<=1;y+=2)for(int z=-1;z<=1;z+=2) m.vertices.add(new Vertex(x,y,z));
        int[][] f = {{0,1,3},{0,3,2},{1,5,7},{1,7,3},{5,4,6},{5,6,7},{4,0,2},{4,2,6},{2,3,7},{2,7,6},{4,5,1},{4,1,0}};
        Color[] c = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN};
        for(int i=0; i<12; i++) m.triangles.add(new Triangle(f[i][0], f[i][1], f[i][2], c[i/2]));
        return m;
    }

    public static Mesh createPyramid() {
        Mesh m = new Mesh();
        m.vertices.add(new Vertex(-1, -1, -1)); m.vertices.add(new Vertex(1, -1, -1));
        m.vertices.add(new Vertex(1, -1, 1)); m.vertices.add(new Vertex(-1, -1, 1));
        m.vertices.add(new Vertex(0, 1, 0)); 
        m.triangles.add(new Triangle(0, 1, 4, Color.ORANGE)); m.triangles.add(new Triangle(1, 2, 4, Color.PINK));
        m.triangles.add(new Triangle(2, 3, 4, Color.WHITE)); m.triangles.add(new Triangle(3, 0, 4, Color.LIGHT_GRAY));
        m.triangles.add(new Triangle(0, 2, 1, Color.GRAY)); m.triangles.add(new Triangle(0, 3, 2, Color.GRAY));
        return m;
    }

    public static Mesh createSphere(double radius, int latLines, int lonLines) {
        Mesh m = new Mesh();
        for (int i = 0; i <= latLines; i++) {
            double phi = Math.PI * i / latLines;
            for (int j = 0; j <= lonLines; j++) {
                double theta = 2 * Math.PI * j / lonLines;
                m.vertices.add(new Vertex(radius * Math.sin(phi) * Math.cos(theta), radius * Math.cos(phi), radius * Math.sin(phi) * Math.sin(theta)));
            }
        }
        for (int i = 0; i < latLines; i++) {
            for (int j = 0; j < lonLines; j++) {
                int f = (i * (lonLines + 1)) + j; int s = f + lonLines + 1;
                m.triangles.add(new Triangle(f, s, f + 1, Color.YELLOW)); m.triangles.add(new Triangle(s, s + 1, f + 1, Color.YELLOW));
            }
        }
        return m;
    }

    public static Mesh createGrid(int size, double tileSize) {
        Mesh m = new Mesh();
        int half = size / 2;
        for (int z = -half; z <= half; z++) {
            for (int x = -half; x <= half; x++) {
                m.vertices.add(new Vertex(x * tileSize, 0, z * tileSize));
            }
        }
        int row = size + 1;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int i = z * row + x;
                Color col = (x + z) % 2 == 0 ? new Color(40, 40, 40) : new Color(60, 60, 60);
                m.triangles.add(new Triangle(i, i + row, i + 1, col));
                m.triangles.add(new Triangle(i + 1, i + row, i + row + 1, col));
            }
        }
        return m;
    }
}

class Matrix4 {
    double[][] m = new double[4][4];
    public Matrix4() { m[0][0]=m[1][1]=m[2][2]=m[3][3]=1; }
    public static Vertex multiply(Matrix4 mat, Vertex v) {
        double x = v.x*mat.m[0][0]+v.y*mat.m[0][1]+v.z*mat.m[0][2]+v.w*mat.m[0][3];
        double y = v.x*mat.m[1][0]+v.y*mat.m[1][1]+v.z*mat.m[1][2]+v.w*mat.m[1][3];
        double z = v.x*mat.m[2][0]+v.y*mat.m[2][1]+v.z*mat.m[2][2]+v.w*mat.m[2][3];
        double w = v.x*mat.m[3][0]+v.y*mat.m[3][1]+v.z*mat.m[3][2]+v.w*mat.m[3][3];
        Vertex res = new Vertex(x,y,z); res.w = w; return res;
    }
    public static Matrix4 multiply(Matrix4 a, Matrix4 b) {
        Matrix4 r = new Matrix4();
        for(int i=0;i<4;i++)for(int j=0;j<4;j++)
            r.m[i][j]=a.m[i][0]*b.m[0][j]+a.m[i][1]*b.m[1][j]+a.m[i][2]*b.m[2][j]+a.m[i][3]*b.m[3][j];
        return r;
    }
    public static Matrix4 projection(double fov, double asp, double n, double f) {
        Matrix4 r = new Matrix4(); double t = 1.0/Math.tan(Math.toRadians(fov)/2);
        r.m[0][0]=t/asp; r.m[1][1]=t; r.m[2][2]=-(f+n)/(f-n); r.m[2][3]=-(2*f*n)/(f-n); r.m[3][2]=-1; r.m[3][3]=0;
        return r;
    }
}

class Transform {
    public double x=0, y=0, z=0, rotX=0, rotY=0, rotZ=0;
    public Matrix4 getModelMatrix() {
        double cx=Math.cos(rotX), sx=Math.sin(rotX), cy=Math.cos(rotY), sy=Math.sin(rotY), cz=Math.cos(rotZ), sz=Math.sin(rotZ);
        Matrix4 r = new Matrix4();
        r.m[0][0]=cy*cz; r.m[0][1]=-cy*sz; r.m[0][2]=sy;
        r.m[1][0]=sx*sy*cz+cx*sz; r.m[1][1]=-sx*sy*sz+cx*cz; r.m[1][2]=-sx*cy;
        r.m[2][0]=-cx*sy*cz+sx*sz; r.m[2][1]=cx*sy*sz+sx*cz; r.m[2][2]=cx*cy;
        Matrix4 t = new Matrix4(); t.m[0][3]=x; t.m[1][3]=y; t.m[2][3]=z;
        return Matrix4.multiply(t, r);
    }
}

class Camera {
    public Transform transform = new Transform();
    public double yaw=0, pitch=0;
    public Matrix4 getViewMatrix() {
        double cY=Math.cos(yaw), sY=Math.sin(yaw), cP=Math.cos(pitch), sP=Math.sin(pitch);
        Matrix4 rY = new Matrix4(); rY.m[0][0]=cY; rY.m[0][2]=sY; rY.m[2][0]=-sY; rY.m[2][2]=cY;
        Matrix4 rX = new Matrix4(); rX.m[1][1]=cP; rX.m[1][2]=-sP; rX.m[2][1]=sP; rX.m[2][2]=cP;
        Matrix4 t = new Matrix4(); t.m[0][3]=-transform.x; t.m[1][3]=-transform.y; t.m[2][3]=-transform.z;
        return Matrix4.multiply(Matrix4.multiply(rX, rY), t);
    }
}

public class SoftwareRenderer extends JPanel implements Runnable, KeyListener, MouseMotionListener {
    private static final int WIDTH = 800, HEIGHT = 600;
    private boolean running = true, wireframe = false, showLightGizmo = true;
    private Camera camera = new Camera();
    private List<GameObject> objects = new ArrayList<>();
    private List<PointLight> lights = new ArrayList<>();
    private GameObject lightGizmo;
    private boolean[] keys = new boolean[256];
    private double[] zBuffer = new double[WIDTH * HEIGHT];
    private Robot robot;
    private BufferedImage frameImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    private int[] pixels = ((DataBufferInt) frameImage.getRaster().getDataBuffer()).getData();

    public static void main(String[] args) {
        JFrame f = new JFrame("Engine 3D - 10.000 Object Stress Test");
        SoftwareRenderer r = new SoftwareRenderer();
        f.add(r); f.pack(); f.setDefaultCloseOperation(3); f.setLocationRelativeTo(null); f.setVisible(true);
        new Thread(r).start();
    }

    public SoftwareRenderer() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true); addKeyListener(this); addMouseMotionListener(this);
        try { robot = new Robot(); } catch (Exception e) {}
        setCursor(getToolkit().createCustomCursor(new BufferedImage(1,1,2), new Point(0,0), ""));
        
        Mesh cubeMesh = Mesh.createCube();
        Mesh pyrMesh = Mesh.createPyramid();
        
        // Criar 10.000 objetos (Grade 100x100)
        for(int i = 0; i < 10000; i++) {
            int row = i / 100;
            int col = i % 100;
            GameObject obj = new GameObject(i % 2 == 0 ? cubeMesh : pyrMesh);
            obj.transform.x = col * 4 - 200;
            obj.transform.z = row * 4 - 200;
            obj.transform.rotY = i * 0.1;
            objects.add(obj);
        }

        GameObject floor = new GameObject(Mesh.createGrid(220, 2.0));
        floor.transform.y = -1.5;
        objects.add(floor);
        
        // 5 LUZES
        lights.add(new PointLight(0, 10, 0, Color.WHITE, 1.8)); 
        lights.add(new PointLight(-180, 15, -180, Color.BLUE, 2.5)); 
        lights.add(new PointLight(180, 15, -180, Color.RED, 2.5));  
        lights.add(new PointLight(180, 15, 180, Color.GREEN, 2.5)); 
        lights.add(new PointLight(-180, 15, 180, Color.CYAN, 2.5));  

        lightGizmo = new GameObject(Mesh.createSphere(0.6, 8, 8));
        camera.transform.z = 60; camera.transform.y = 15;
    }

    private void update() {
        for(int i=0; i < 10000; i++) {
            objects.get(i).transform.rotY += (i % 2 == 0 ? 0.04 : -0.03);
        }

        PointLight spot = lights.get(0);
        if(keys[KeyEvent.VK_UP]) spot.pos.z -= 1.5; if(keys[KeyEvent.VK_DOWN]) spot.pos.z += 1.5;
        if(keys[KeyEvent.VK_LEFT]) spot.pos.x -= 1.5; if(keys[KeyEvent.VK_RIGHT]) spot.pos.x += 1.5;
        if(keys[KeyEvent.VK_I]) spot.pos.y += 1.0; if(keys[KeyEvent.VK_K]) spot.pos.y -= 1.0;

        lightGizmo.transform.x = spot.pos.x; lightGizmo.transform.y = spot.pos.y; lightGizmo.transform.z = spot.pos.z;

        double sp = 0.8; // Velocidade maior para o mapa gigante
        double sY = Math.sin(camera.yaw), cY = Math.cos(camera.yaw);
        double fX = sY, fZ = -cY, rX = cY, rZ = sY;

        if(keys[KeyEvent.VK_W]){ camera.transform.x += fX * sp; camera.transform.z += fZ * sp; }
        if(keys[KeyEvent.VK_S]){ camera.transform.x -= fX * sp; camera.transform.z -= fZ * sp; }
        if(keys[KeyEvent.VK_A]){ camera.transform.x -= rX * sp; camera.transform.z -= rZ * sp; }
        if(keys[KeyEvent.VK_D]){ camera.transform.x += rX * sp; camera.transform.z += rZ * sp; }
    }

    @Override public void run() { while(running){ update(); repaint(); try{Thread.sleep(16);}catch(Exception e){} } }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Arrays.fill(pixels, 0); Arrays.fill(zBuffer, Double.POSITIVE_INFINITY);
        for(GameObject obj : objects) obj.draw(pixels, zBuffer, camera, lights, WIDTH, HEIGHT, wireframe);
        if (showLightGizmo) lightGizmo.draw(pixels, zBuffer, camera, null, WIDTH, HEIGHT, true);
        g.drawImage(frameImage, 0, 0, null);
        
        g.setColor(Color.WHITE);
        g.drawString("OBJETOS ATIVOS: 10.000", 10, 20);
        g.drawString("TRIÂNGULOS ESTIMADOS: ~120.000", 10, 40);
    }

    class GameObject {
        Mesh mesh; Transform transform = new Transform();
        GameObject(Mesh m) { mesh = m; }

        void draw(int[] pixels, double[] zBuf, Camera cam, List<PointLight> sceneLights, int w, int h, boolean wire) {
            Matrix4 view = cam.getViewMatrix(); Matrix4 model = transform.getModelMatrix();
            Matrix4 proj = Matrix4.projection(90, (double)w/h, 0.01, 3000);
            Matrix4 modelView = Matrix4.multiply(view, model);

            for (Triangle t : mesh.triangles) {
                Vertex v1 = Matrix4.multiply(modelView, mesh.vertices.get(t.v1));
                Vertex v2 = Matrix4.multiply(modelView, mesh.vertices.get(t.v2));
                Vertex v3 = Matrix4.multiply(modelView, mesh.vertices.get(t.v3));

                double nx = (v2.y-v1.y)*(v3.z-v1.z) - (v2.z-v1.z)*(v3.y-v1.y);
                double ny = (v2.z-v1.z)*(v3.x-v1.x) - (v2.x-v1.x)*(v3.z-v1.z);
                double nz = (v2.x-v1.x)*(v3.y-v1.y) - (v2.y-v1.y)*(v3.x-v1.x);
                double len = Math.sqrt(nx*nx + ny*ny + nz*nz);
                if(len > 0) { nx/=len; ny/=len; nz/=len; }

                if (nx*v1.x + ny*v1.y + nz*v1.z < 0) {
                    double rT=0, gT=0, bT=0, amb = 0.1;
                    double mx = (v1.x+v2.x+v3.x)/3.0, my = (v1.y+v2.y+v3.y)/3.0, mz = (v1.z+v2.z+v3.z)/3.0;

                    if (sceneLights != null && !wire) {
                        for(PointLight light : sceneLights) {
                            Vertex lV = Matrix4.multiply(view, light.pos);
                            double lx = lV.x-mx, ly = lV.y-my, lz = lV.z-mz;
                            double d = Math.sqrt(lx*lx+ly*ly+lz*lz);
                            double dot = Math.max(0, nx*(lx/d) + ny*(ly/d) + nz*(lz/d));
                            double att = 1.0 / (1.0 + 0.003*d*d);
                            rT += (t.baseColor.getRed()/255.0) * (light.color.getRed()/255.0) * dot * light.intensity * att;
                            gT += (t.baseColor.getGreen()/255.0) * (light.color.getGreen()/255.0) * dot * light.intensity * att;
                            bT += (t.baseColor.getBlue()/255.0) * (light.color.getBlue()/255.0) * dot * light.intensity * att;
                        }
                    }

                    int finalCol = new Color((int)(Math.min(1, rT+amb)*255), (int)(Math.min(1, gT+amb)*255), (int)(Math.min(1, bT+amb)*255)).getRGB();

                    Vertex[] p = new Vertex[3]; Vertex[] orig = {v1, v2, v3}; boolean clip = false;
                    for (int i=0; i<3; i++) {
                        Vertex pr = Matrix4.multiply(proj, orig[i]);
                        if (pr.w <= 0.01) { clip=true; break; }
                        p[i] = new Vertex((pr.x/pr.w+1)*w/2, (1-pr.y/pr.w)*h/2, 1.0/pr.w);
                    }
                    if (!clip) {
                        if (wire) drawWireframe(pixels, p, t.baseColor.getRGB(), w, h);
                        else rasterize(pixels, zBuf, p, finalCol, w, h);
                    }
                }
            }
        }

        void rasterize(int[] pixels, double[] zBuf, Vertex[] v, int color, int w, int h) {
            int minX = (int)Math.max(0, Math.min(v[0].x, Math.min(v[1].x, v[2].x)));
            int maxX = (int)Math.min(w-1, Math.max(v[0].x, Math.max(v[1].x, v[2].x)));
            int minY = (int)Math.max(0, Math.min(v[0].y, Math.min(v[1].y, v[2].y)));
            int maxY = (int)Math.min(h-1, Math.max(v[0].y, Math.max(v[1].y, v[2].y)));
            double area = (v[1].y-v[2].y)*(v[0].x-v[2].x) + (v[2].x-v[1].x)*(v[0].y-v[2].y);
            if(area == 0) return;
            for (int y=minY; y<=maxY; y++) {
                for (int x=minX; x<=maxX; x++) {
                    double w0 = ((v[1].y-v[2].y)*(x-v[2].x)+(v[2].x-v[1].x)*(y-v[2].y))/area;
                    double w1 = ((v[2].y-v[0].y)*(x-v[2].x)+(v[0].x-v[2].x)*(y-v[2].y))/area;
                    double w2 = 1.0-w0-w1;
                    if (w0>=0 && w1>=0 && w2>=0) {
                        double d = w0*v[0].z + w1*v[1].z + w2*v[2].z;
                        int idx = y*w+x;
                        if (d > (1.0/zBuf[idx])) { zBuf[idx]=1.0/d; pixels[idx]=color; }
                    }
                }
            }
        }

        void drawWireframe(int[] pixels, Vertex[] v, int color, int w, int h) {
            drawLine(pixels, v[0], v[1], color, w, h); drawLine(pixels, v[1], v[2], color, w, h); drawLine(pixels, v[2], v[0], color, w, h);
        }

        void drawLine(int[] pixels, Vertex v1, Vertex v2, int color, int w, int h) {
            int x0=(int)v1.x, y0=(int)v1.y, x1=(int)v2.x, y1=(int)v2.y;
            int dx=Math.abs(x1-x0), dy=Math.abs(y1-y0), sx=x0<x1?1:-1, sy=y0<y1?1:-1, err=dx-dy;
            while(true){
                if(x0>=0 && x0<w && y0>=0 && y0<h) pixels[y0*w+x0]=color;
                if(x0==x1 && y0==y1) break;
                int e2=2*err; if(e2>-dy){err-=dy; x0+=sx;} if(e2<dx){err+=dx; y0+=sy;}
            }
        }
    }

    public void mouseMoved(MouseEvent e) {
        if(!isFocusOwner()) return;
        Point c=getLocationOnScreen(); camera.yaw+=(e.getXOnScreen()-(c.x+WIDTH/2))*0.003;
        camera.pitch-=(e.getYOnScreen()-(c.y+HEIGHT/2))*0.003;
        camera.pitch=Math.max(-1.5, Math.min(1.5, camera.pitch));
        robot.mouseMove(c.x+WIDTH/2, c.y+HEIGHT/2);
    }
    public void keyPressed(KeyEvent e){ 
        if(e.getKeyCode()==KeyEvent.VK_F2) wireframe=!wireframe;
        if(e.getKeyCode()==KeyEvent.VK_F3) showLightGizmo=!showLightGizmo;
        if(e.getKeyCode()<256) keys[e.getKeyCode()]=true; 
    }
    public void keyReleased(KeyEvent e){ if(e.getKeyCode()<256) keys[e.getKeyCode()]=false; }
    public void keyTyped(KeyEvent e){}
    public void mouseDragged(MouseEvent e){ mouseMoved(e); }
}