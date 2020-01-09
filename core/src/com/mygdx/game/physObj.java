package com.mygdx.game;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import com.badlogic.gdx.physics.bullet.dynamics.*;
import com.badlogic.gdx.physics.bullet.linearmath.*;
import com.badlogic.gdx.physics.bullet.collision.*;

import java.util.ArrayList;

public class physObj {



    public static class MotionState extends btMotionState {

        public Matrix4 transform;

        public MotionState(final Matrix4 transform) {
            this.transform = transform;
        }

        @Override
        public void getWorldTransform(Matrix4 worldTrans) {
            worldTrans.set(transform);
        }

        @Override
        public void setWorldTransform(Matrix4 worldTrans) {
            transform.set(worldTrans);
        }
    }

    public MotionState motionstate;
    public static Vector3 tmp = new Vector3();

    public static ArrayList<physObj> physObjects = new ArrayList<physObj>();

    public enum pType {
        SPHERE, BOX
    };

    public static Model boxTemplateModel;
    public static Model ballTemplateModel;
    public ModelInstance modelInst;

    public btCollisionShape shape;
    public btRigidBody.btRigidBodyConstructionInfo bodyInfo;
    public btRigidBody body;
    public static btDynamicsWorld collisionWorld;
    public //final
            Vector3 scale;


    public physObj(){ // mt
    }

//    public physObj(pType tp, Vector3 sz, float mass, final Matrix4 transform) {
//
//        create(tp, sz, mass, transform);
//    }


// only for the landscape right now
    public void addBody( Vector3 sz){

        float mass = 0;

        scale = sz.cpy();

        if (mass == 0) {
            modelInst.transform.scl(sz);
            tmp = Vector3.Zero; // Vector3.Zero.copy()  ?
            motionstate = null;
        } else {
            shape.calculateLocalInertia(mass, tmp);
            motionstate = new MotionState(modelInst.transform);
        }

        bodyInfo = new btRigidBody.btRigidBodyConstructionInfo(mass, motionstate, shape, tmp.cpy());
        body = new btRigidBody(bodyInfo);
        body.setFriction(0.8f);

        if (mass == 0) {
            body.translate(tmp.set(modelInst.transform.val[12], modelInst.transform.val[13], modelInst.transform.val[14]));
        }

        collisionWorld.addRigidBody(body);

        physObjects.add(this);
    }

    public void dispose() {
        if (null != body) { // no body added for comp shape
            body.dispose();
        }

        shape.dispose();

        if (null != bodyInfo) { // no body info added for comp shape
            bodyInfo.dispose();
        }
        //		motionstate.dispose();  body deletion does this?

        physObjects.remove(this);
    }
}
