package com.james.status.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

import com.james.status.utils.StaticUtils;

import androidx.annotation.ColorInt;

public class ColorView extends RenderableView {

    @ColorInt
    int color = Color.BLACK;
    float outlineSize;
    Paint tilePaint;

    public ColorView(final Context context) {
        super(context);
        setUp();
    }

    public ColorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setUp();
    }

    public ColorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setUp();
    }

    void setUp() {
        outlineSize = StaticUtils.getPixelsFromDp(2);

        tilePaint = new Paint();
        tilePaint.setAntiAlias(true);
        tilePaint.setStyle(Paint.Style.FILL);
        tilePaint.setColor(Color.LTGRAY);
    }

    public void setColor(@ColorInt int color) {
        this.color = color;
        startRender();
    }

    @Override
    public void render(Canvas canvas) {
        if (Color.alpha(color) < 255) {
            int outline = Math.round(outlineSize) * 4;
            for (int x = 0; x < canvas.getWidth(); x += outline) {
                for (int y = x % (outline * 2) == 0 ? 0 : outline; y < canvas.getWidth(); y += (outline * 2)) {
                    canvas.drawRect(x, y, x + outline, y + outline, tilePaint);
                }
            }
        }

        canvas.drawColor(color);
    }
}
