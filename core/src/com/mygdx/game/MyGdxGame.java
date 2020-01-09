package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseInterface;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;

import java.util.Iterator;
import java.util.Random;

public class MyGdxGame extends ApplicationAdapter {

    public Environment environment;
    public PerspectiveCamera cam;
    public Model model;
    public ModelBatch modelBatch;

    public CameraInputController camController;
    public AssetManager assets;

    public Vector3 tmpV = new Vector3();
    public Matrix4 tmpM = new Matrix4();

    btCollisionConfiguration collisionConfiguration;
    btCollisionDispatcher dispatcher;
    btBroadphaseInterface broadphase;
    btConstraintSolver solver;
    btDynamicsWorld collisionWorld;

    private Model landscapeModel;
    private ModelInstance landscapeInstance;

    Vector3 gravity = new Vector3(0, -9.81f, 0);
    public Random rnd = new Random();
    private final ModelBuilder modelBuilder = new ModelBuilder();

    private btCompoundShape compoundShape;
    private physObj.MotionState compMotionstate;
    private btRigidBody.btRigidBodyConstructionInfo compBodyInfo;
    private btRigidBody compBody;

    private DebugDrawer debugDrawer;
    private final boolean USE_DDBUG_DRAW = true;
    private final boolean TRY_COMP_SHAPE = true;
    private Vector3 compBodyPosition = new Vector3();


    @Override
    public void render() {

        camController.update();
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // TODO variable timesteps are usually not good for physics sims
        // but good enough for a quick example... (should really accrue delta
        // time and decrement chunks of sim steps)
        // however for the time being bullet seems to be coping better than
        // expected with variable(ish) timesteps
        // increase itterations for more accurate but slower sim
        collisionWorld.stepSimulation(Gdx.graphics.getDeltaTime(), 5);

        if (TRY_COMP_SHAPE) {
// update mesh parts to compound shape
            Matrix4 compBodyTransform = compBody.getWorldTransform();
            compBodyTransform.getTranslation(compBodyPosition);

// for each child shapes, link back to it's model instance thru the object
            for (int x = 0; x < compoundShape.getNumChildShapes(); x++) {

                btCollisionShape bcs = compoundShape.getChildShape(x);
                physObj pob = physObj.physObjects.get(bcs.getUserIndex());
                ModelInstance mi = pob.modelInst;

                // assumes only the single node in this model instance
                tmpV = mi.nodes.get(0).localTransform.getTranslation(tmpV);
                mi.transform.set(compBodyTransform);
                mi.transform.translate(tmpV);
            }
        }
/*
 iterate the renderables as usual (note compound body is of course NOT in this set)
 */
        modelBatch.begin(cam);

        Iterator<physObj> it = physObj.physObjects.iterator();
        while (it.hasNext()) {
            physObj pob = it.next();

//			if (pob.body.isActive()) {  // gdx bullet used to leave scaling alone which was rather useful...
//				pob.modelInst.transform.mul(tmpM.setToScaling(pob.scale));
//				pob.motionstate.getWorldTransform(tmpM);
//				tmpM.getTranslation(tmpV);
//				if (tmpV.y<-10) {
//					tmpM.setToTranslation(rnd.nextFloat() * 10.0f - 5f, rnd.nextFloat() + 25f, rnd.nextFloat() * 10.0f - 5f);
//					pob.body.setWorldTransform(tmpM);
//					pob.body.setAngularVelocity(Vector3.Zero);
//					pob.body.setLinearVelocity(Vector3.Zero);
//				}
//			}
            modelBatch.render(pob.modelInst, environment);
        }

        modelBatch.render(landscapeInstance, environment);
        modelBatch.end();

        debugDrawer.begin(cam);
        collisionWorld.debugDrawWorld();
        debugDrawer.end();
    }

    @Override
    public void create() {
        modelBatch = new ModelBatch();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(-5f, 6f, 19f);
        cam.lookAt(0, 0, 0);
        cam.near = 0.1f;
        cam.far = 300f;
        cam.update();

        Bullet.init();
        // Create the bullet world
        collisionConfiguration = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfiguration);
        broadphase = new btDbvtBroadphase();
        solver = new btSequentialImpulseConstraintSolver();
        collisionWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);
        collisionWorld.setGravity(gravity);

        assets = new AssetManager();
        assets.load("data/landscape.g3db", Model.class);

        camController = new CameraInputController(cam);
        Gdx.input.setInputProcessor(camController);


        assets.finishLoading();

        //
        // here onwards all assets ready!
        //


        Texture cubeTex = new Texture(Gdx.files.internal("data/crate.png"), false);
        Model cube = modelBuilder.createBox(2f, 2f, 2f,
                new Material(TextureAttribute.createDiffuse(cubeTex)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
        physObj.boxTemplateModel = cube;  // must set the visual templates before using.

        Texture sphereTex = new Texture(Gdx.files.internal("data/day.png"), false);
        Model ball = modelBuilder.createSphere(2f, 2f, 2f, 16, 16,
                new Material(TextureAttribute.createDiffuse(sphereTex)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
        physObj.ballTemplateModel = ball;

        physObj.collisionWorld = collisionWorld;


        // little point putting static meshes in a convenience wrapper
        // as you only have a few and don't spawn them repeatedly

        landscapeModel = assets.get("data/landscape.g3db", Model.class);
        btCollisionShape triMesh = (btCollisionShape) new btBvhTriangleMeshShape(landscapeModel.meshParts);
        // put the landscape at an angle so stuff falls of it...
        physObj.MotionState motionstate = new physObj.MotionState(new Matrix4().idt().rotate(new Vector3(1, 0, 0), 20f));
        btRigidBody landscape = new btRigidBody(0, motionstate, triMesh);
        landscapeInstance = new ModelInstance(landscapeModel);
        landscapeInstance.transform = motionstate.transform;
        collisionWorld.addRigidBody(landscape);


        compoundShape = new btCompoundShape();
        physObj physicsObject;

        // uncomment for a terrain alternative;
        tmpM.idt().trn(0, -15, 15);
//		tmpM.idt().rotate(new Vector3(1,0,0), 20f);                      ///// NFG !!!!!!!!!!!!!!!!!!!!!!!
//		new physObj(physObj.pType.BOX, tmpV.set(20f, 1f, 20f), 0, tmpM);	// zero mass = static
        physicsObject = new physObj();
        tmpV.set(10f, 0.5f, 10f);
        physicsObject.shape = new btBoxShape(tmpV);
        physicsObject.modelInst = new ModelInstance(physObj.boxTemplateModel);
        physicsObject.modelInst.transform = tmpM.cpy();
//		physicsObject.scale = tmpV.cpy();
        physicsObject.addBody(tmpV);


        float mass = 1;
        Vector3 inertia;
// box shape
        physicsObject = new physObj();
        physObj.physObjects.add(physicsObject); // add to the phys objects for modelinstance rendering
        Vector3 size = new Vector3(1, 1, 1);
        physicsObject.shape = new btBoxShape(size);
        physicsObject.shape.setUserIndex(physObj.physObjects.size());
        compoundShape.addChildShape(new Matrix4().idt().trn(1f, 1, 0), physicsObject.shape);
        physicsObject.modelInst = new ModelInstance(physObj.boxTemplateModel);
        physicsObject.modelInst.transform = new Matrix4().trn(1, 9, 1);  // trn only needed for testing non-compound shapes
        physicsObject.modelInst.nodes.get(0).localTransform.setToTranslation(1f, 1, 0); // this will be set from the model
        physicsObject.scale = tmpV.cpy();

        if (!TRY_COMP_SHAPE) {
            inertia = new Vector3();
            physicsObject.shape.calculateLocalInertia(mass, inertia);
            physicsObject.motionstate = new physObj.MotionState(physicsObject.modelInst.transform);
            physicsObject.bodyInfo =
                    new btRigidBody.btRigidBodyConstructionInfo(mass, physicsObject.motionstate, physicsObject.shape, inertia);
            physicsObject.body = new btRigidBody(physicsObject.bodyInfo);
            physicsObject.body.setFriction(0.8f);
            collisionWorld.addRigidBody(physicsObject.body);
        }

// ball shape
        physicsObject = new physObj();
        physObj.physObjects.add(physicsObject); // add to the phys objects for modelinstance rendering
        physicsObject.shape = new btSphereShape(1);
        physicsObject.shape.setUserIndex(physObj.physObjects.size());
        compoundShape.addChildShape(new Matrix4().idt().trn(0, 0, 0), physicsObject.shape);
        physicsObject.modelInst = new ModelInstance(physObj.ballTemplateModel);
        physicsObject.modelInst.transform = new Matrix4().trn(0, 9, 0); // trn only needed for testing non-compound shapes
        physicsObject.modelInst.nodes.get(0).localTransform.setToTranslation(0, 0, 0); // this will be set from the model
        physicsObject.scale = tmpV.cpy();

        if (!TRY_COMP_SHAPE) {
            inertia = new Vector3();
            physicsObject.shape.calculateLocalInertia(mass, inertia);
            physicsObject.motionstate = new physObj.MotionState(physicsObject.modelInst.transform);
            physicsObject.bodyInfo =
                    new btRigidBody.btRigidBodyConstructionInfo(mass, physicsObject.motionstate, physicsObject.shape, inertia);
            physicsObject.body = new btRigidBody(physicsObject.bodyInfo);
            physicsObject.body.setFriction(0.8f);
            collisionWorld.addRigidBody(physicsObject.body);
        }


// create compound shape and body
///
        if (TRY_COMP_SHAPE) {
            inertia = new Vector3();
            mass = 1 + 1; // for the two objects of unit mass that I am combining
            compoundShape.calculateLocalInertia(mass, inertia);

            Matrix4 compTransform = new Matrix4().trn(0, 9, 0);
            compMotionstate = new physObj.MotionState(compTransform);

            compBodyInfo =
                    new btRigidBody.btRigidBodyConstructionInfo(
                            mass, compMotionstate, compoundShape, inertia);
            compBody = new btRigidBody(compBodyInfo);
            compBody.setFriction(0.8f);
            collisionWorld.addRigidBody(compBody);
        }
///

        debugDrawer = new DebugDrawer();
        debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_MAX_DEBUG_DRAW_MODE);

        if (USE_DDBUG_DRAW) {
            collisionWorld.setDebugDrawer(debugDrawer);
        }
    }

    @Override
    public void resize(int width, int height) {
    }


    @Override
    public void dispose() {

        debugDrawer.dispose();
        compoundShape.dispose();
        if (null != compBodyInfo)
            compBodyInfo.dispose();

        collisionWorld.dispose();
        solver.dispose();
        broadphase.dispose();
        dispatcher.dispose();
        collisionConfiguration.dispose();

        Iterator<physObj> it = physObj.physObjects.iterator();
        while (it.hasNext()) {
            physObj pob = it.next();
            // doing it like this to avoid comodification...
            it.remove();
            pob.dispose();
        }
        modelBatch.dispose();
        assets.dispose();
    }
}
