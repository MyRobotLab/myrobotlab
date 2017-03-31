package org.saintandreas.gl;

import org.saintandreas.math.Matrix3f;
import org.saintandreas.math.Matrix4f;
import org.saintandreas.math.Quaternion;
import org.saintandreas.math.Vector2f;
import org.saintandreas.math.Vector3f;

public interface Transformable<T extends Transformable<T>> {
  public Matrix4f getTransform();

  public T identity();

  public T transpose();

  public T multiply(Matrix4f m);

  public T preMultiply(Matrix4f m);

  public T translate(float x);

  public T translate(Vector2f vec);

  public T translate(Vector3f vec);

  public T preTranslate(float x);

  public T preTranslate(Vector2f v);

  public T preTranslate(Vector3f v);

  public T rotate(float angle, Vector3f axis);

  public T rotate(Quaternion q);

  public T rotate(Matrix3f m);

  public T preRotate(float angle, Vector3f axis);

  public T preRotate(Quaternion q);

  public T preRotate(Matrix3f m);

  public T scale(float f);

  public T scale(Vector3f vec);

  public T orthographic(float left, float right, float bottom, float top, float near, float far);

  public T lookat(Vector3f eye, Vector3f center, Vector3f up);

  public T perspective(float fovy, float aspect, float zNear, float zFar);
}
