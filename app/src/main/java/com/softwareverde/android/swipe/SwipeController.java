package com.softwareverde.android.swipe;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.support.v7.widget.helper.ItemTouchHelper.Callback;
import android.view.MotionEvent;
import android.view.View;

import static android.support.v7.widget.helper.ItemTouchHelper.ACTION_STATE_SWIPE;
import static android.support.v7.widget.helper.ItemTouchHelper.LEFT;
import static android.support.v7.widget.helper.ItemTouchHelper.RIGHT;

enum ButtonsState {
    GONE,
    LEFT_VISIBLE,
    RIGHT_VISIBLE
}

public class SwipeController extends Callback {
    public interface OnButtonClickedCallback {
        void onClick(int itemPosition);
    }

    public static class Button {
        public float width = 300F;
        public int backgroundColor = Color.parseColor("#FAFAFA");
        public int textColor = Color.parseColor("#FFFFFF");
        public float textSize = 60F;
        public Typeface fontFace = Typeface.SANS_SERIF;
        public String text = "";
        public OnButtonClickedCallback onButtonClickedCallback;
    }

    protected int _backgroundColor = Color.parseColor("#FAFAFA");
    protected Button _leftButton = null;
    protected Button _rightButton = null;

    protected boolean _swipeBack = false;
    protected ButtonsState _buttonShowedState = ButtonsState.GONE;
    protected RectF _buttonInstance = null;

    protected Long _currentViewId = null;
    protected RecyclerView.ViewHolder _currentItemViewHolder = null;

    protected void _setTouchListener(final Canvas canvas, final RecyclerView recyclerView, final RecyclerView.ViewHolder viewHolder, final float dX, final float dY, final int actionState, final boolean isCurrentlyActive) {
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                _swipeBack = ( (event.getAction() == MotionEvent.ACTION_CANCEL) || (event.getAction() == MotionEvent.ACTION_UP) );
                if (_swipeBack) {
                    if (dX < -(_rightButton != null ? _rightButton.width : Float.MAX_VALUE)) {
                        _buttonShowedState = ButtonsState.RIGHT_VISIBLE;
                    }
                    else if (dX > (_leftButton != null ? _leftButton.width : Float.MAX_VALUE)) {
                        _buttonShowedState = ButtonsState.LEFT_VISIBLE;
                    }

                    if (_buttonShowedState != ButtonsState.GONE) {
                        SwipeController.this._setTouchDownListener(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                        SwipeController.this._setItemsClickable(recyclerView, false);
                    }
                }
                return false;
            }
        });
    }

    protected void _setTouchDownListener(final Canvas canvas, final RecyclerView recyclerView, final RecyclerView.ViewHolder viewHolder, final float dX, final float dY, final int actionState, final boolean isCurrentlyActive) {
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    _setTouchUpListener(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
                return false;
            }
        });
    }

    protected void _setTouchUpListener(final Canvas canvas, final RecyclerView recyclerView, final RecyclerView.ViewHolder viewHolder, final float dX, final float dY, final int actionState, final boolean isCurrentlyActive) {
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    SwipeController.super.onChildDraw(canvas, recyclerView, viewHolder, 0F, dY, actionState, isCurrentlyActive);
                    recyclerView.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(final View view, final MotionEvent event) { return false; }
                    });
                    _setItemsClickable(recyclerView, true);
                    _swipeBack = false;

                    if ( (_buttonInstance != null) && (_buttonInstance.contains(event.getX(), event.getY())) ) {
                        final int itemPosition = viewHolder.getAdapterPosition();
                        if ( (_buttonShowedState == ButtonsState.LEFT_VISIBLE) && (_leftButton != null) ) {
                            if (_leftButton.onButtonClickedCallback != null) {
                                _leftButton.onButtonClickedCallback.onClick(itemPosition);
                            }
                        }
                        else if ( (_buttonShowedState == ButtonsState.RIGHT_VISIBLE) && (_rightButton != null)) {
                            if (_rightButton.onButtonClickedCallback != null) {
                                _rightButton.onButtonClickedCallback.onClick(itemPosition);
                            }
                        }
                    }

                    _buttonShowedState = ButtonsState.GONE;
                    _currentViewId = null;
                    _currentItemViewHolder = null;
                }
                return false;
            }
        });
    }

    protected void _setItemsClickable(final RecyclerView recyclerView, final boolean isClickable) {
        for (int i = 0; i < recyclerView.getChildCount(); ++i) {
            recyclerView.getChildAt(i).setClickable(isClickable);
        }
    }

    protected void _drawButtons(final Canvas canvas, final RecyclerView.ViewHolder viewHolder) {
        final float corners = 16;

        final View itemView = viewHolder.itemView;
        final Paint paint = new Paint();

        canvas.drawColor(_backgroundColor);

        if ( (_buttonShowedState == ButtonsState.LEFT_VISIBLE) && (_leftButton != null)) {
            final float buttonWidthWithoutPadding = (_leftButton.width - 20);
            final RectF leftButton = new RectF(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + buttonWidthWithoutPadding, itemView.getBottom());
            {
                paint.setColor(_leftButton.backgroundColor);
                canvas.drawRoundRect(leftButton, corners, corners, paint);

                paint.setColor(_leftButton.textColor);
                _drawText(_leftButton.text, _leftButton.textSize, _leftButton.fontFace, canvas, leftButton, paint);
            }

            _buttonInstance = leftButton;
        }
        else if ( (_buttonShowedState == ButtonsState.RIGHT_VISIBLE) && (_rightButton != null) ) {
            final float buttonWidthWithoutPadding = (_rightButton.width - 20);
            final RectF rightButton = new RectF(itemView.getRight() - buttonWidthWithoutPadding, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            {
                paint.setColor(_rightButton.backgroundColor);
                canvas.drawRoundRect(rightButton, corners, corners, paint);

                paint.setColor(_rightButton.textColor);
                _drawText(_rightButton.text, _rightButton.textSize, _rightButton.fontFace, canvas, rightButton, paint);
            }

            _buttonInstance = rightButton;
        }
        else {
            _buttonInstance = null;
        }
    }

    protected void _drawText(final String text, final float textSize, final Typeface typeFace, final Canvas canvas, final RectF button, final Paint paint) {
        paint.setAntiAlias(true);
        paint.setTextSize(textSize);
        paint.setTypeface(typeFace);

        final float textWidth = paint.measureText(text);
        canvas.drawText(text, (button.centerX() - (textWidth / 2)), (button.centerY() + (textSize / 2)), paint);
    }

    public SwipeController() { }

    public void setLeftButton(final Button button) {
        _leftButton = button;
    }

    public void setRightButton(final Button button) {
        _rightButton = button;
    }

    public void setBackgroundColor(final int backgroundColor) {
        _backgroundColor = backgroundColor;
    }

    @Override
    public int getMovementFlags(final RecyclerView recyclerView, final RecyclerView.ViewHolder viewHolder) {
        return ItemTouchHelper.Callback.makeMovementFlags(0, ((_leftButton != null ? RIGHT : 0) | (_rightButton != null ? LEFT : 0)));
    }

    @Override
    public boolean onMove(final RecyclerView recyclerView, final RecyclerView.ViewHolder viewHolder, final RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(final RecyclerView.ViewHolder viewHolder, final int direction) { }

    @Override
    public int convertToAbsoluteDirection(final int flags, final int layoutDirection) {
        if (_swipeBack) {
            _swipeBack = (_buttonShowedState != ButtonsState.GONE);
            return 0;
        }
        return super.convertToAbsoluteDirection(flags, layoutDirection);
    }

    @Override
    public void onChildDraw(final Canvas canvas, final RecyclerView recyclerView, final RecyclerView.ViewHolder viewHolder, float dX, float dY, final int actionState, final boolean isCurrentlyActive) {
        if (actionState == ACTION_STATE_SWIPE) {
            if (_buttonShowedState != ButtonsState.GONE) {
                if (_buttonShowedState == ButtonsState.LEFT_VISIBLE) {
                    dX = Math.max(dX, (_leftButton != null ? _leftButton.width : 0F));
                }

                if (_buttonShowedState == ButtonsState.RIGHT_VISIBLE) {
                    dX = Math.min(dX, -(_rightButton != null ? _rightButton.width : 0F));
                }

                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
            else {
                this._setTouchListener(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }

        if (_buttonShowedState == ButtonsState.GONE) {
            super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        _currentViewId = viewHolder.getItemId();
        _currentItemViewHolder = viewHolder;
    }

    public void onDraw(final Canvas canvas, final RecyclerView recyclerView, final RecyclerView.State state) {
        if ( (_currentItemViewHolder != null) ) {
            if ((_currentItemViewHolder.getItemId() == _currentViewId)) {
                _drawButtons(canvas, _currentItemViewHolder);
            }
            else {
                super.onChildDraw(canvas, recyclerView, _currentItemViewHolder, 0, 0, 0, false);
                _currentViewId = null;
                _currentItemViewHolder = null;
            }
        }
        else {
            _currentViewId = null;
            _currentItemViewHolder = null;
        }
    }
}