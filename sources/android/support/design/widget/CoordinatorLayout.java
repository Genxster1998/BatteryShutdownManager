package android.support.design.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.design.R;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import com.anjlab.android.iab.v3.Constants;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoordinatorLayout extends ViewGroup implements NestedScrollingParent {
    static final Class<?>[] CONSTRUCTOR_PARAMS = {Context.class, AttributeSet.class};
    static final CoordinatorLayoutInsetsHelper INSETS_HELPER;
    static final String TAG = "CoordinatorLayout";
    static final Comparator<View> TOP_SORTED_CHILDREN_COMPARATOR;
    static final String WIDGET_PACKAGE_NAME = CoordinatorLayout.class.getPackage().getName();
    static final ThreadLocal<Map<String, Constructor<Behavior>>> sConstructors = new ThreadLocal<>();
    private View mBehaviorTouchView;
    private final List<View> mDependencySortedChildren;
    private boolean mDrawStatusBarBackground;
    private boolean mIsAttachedToWindow;
    private int[] mKeylines;
    private WindowInsetsCompat mLastInsets;
    final Comparator<View> mLayoutDependencyComparator;
    private boolean mNeedsPreDrawListener;
    private View mNestedScrollingDirectChild;
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private View mNestedScrollingTarget;
    private OnPreDrawListener mOnPreDrawListener;
    private Paint mScrimPaint;
    private Drawable mStatusBarBackground;
    private final List<View> mTempDependenciesList;
    private final int[] mTempIntPair;
    private final List<View> mTempList1;
    private final Rect mTempRect1;
    private final Rect mTempRect2;
    private final Rect mTempRect3;

    @Retention(RetentionPolicy.RUNTIME)
    public @interface DefaultBehavior {
        Class<? extends Behavior> value();
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v5, resolved type: java.lang.Class<?>[]} */
    /* JADX WARNING: Multi-variable type inference failed */
    static {
        /*
            r2 = 0
            java.lang.Class<android.support.design.widget.CoordinatorLayout> r0 = android.support.design.widget.CoordinatorLayout.class
            java.lang.Package r0 = r0.getPackage()
            java.lang.String r0 = r0.getName()
            WIDGET_PACKAGE_NAME = r0
            int r0 = android.os.Build.VERSION.SDK_INT
            r1 = 21
            if (r0 < r1) goto L_0x0038
            android.support.design.widget.CoordinatorLayout$ViewElevationComparator r0 = new android.support.design.widget.CoordinatorLayout$ViewElevationComparator
            r0.<init>()
            TOP_SORTED_CHILDREN_COMPARATOR = r0
            android.support.design.widget.CoordinatorLayoutInsetsHelperLollipop r0 = new android.support.design.widget.CoordinatorLayoutInsetsHelperLollipop
            r0.<init>()
            INSETS_HELPER = r0
        L_0x0021:
            r0 = 2
            java.lang.Class[] r0 = new java.lang.Class[r0]
            r1 = 0
            java.lang.Class<android.content.Context> r2 = android.content.Context.class
            r0[r1] = r2
            r1 = 1
            java.lang.Class<android.util.AttributeSet> r2 = android.util.AttributeSet.class
            r0[r1] = r2
            CONSTRUCTOR_PARAMS = r0
            java.lang.ThreadLocal r0 = new java.lang.ThreadLocal
            r0.<init>()
            sConstructors = r0
            return
        L_0x0038:
            TOP_SORTED_CHILDREN_COMPARATOR = r2
            INSETS_HELPER = r2
            goto L_0x0021
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.design.widget.CoordinatorLayout.<clinit>():void");
    }

    public CoordinatorLayout(Context context) {
        this(context, (AttributeSet) null);
    }

    public CoordinatorLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CoordinatorLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mLayoutDependencyComparator = new Comparator<View>() {
            public int compare(View lhs, View rhs) {
                if (lhs == rhs) {
                    return 0;
                }
                if (((LayoutParams) lhs.getLayoutParams()).dependsOn(CoordinatorLayout.this, lhs, rhs)) {
                    return 1;
                }
                return ((LayoutParams) rhs.getLayoutParams()).dependsOn(CoordinatorLayout.this, rhs, lhs) ? -1 : 0;
            }
        };
        this.mDependencySortedChildren = new ArrayList();
        this.mTempList1 = new ArrayList();
        this.mTempDependenciesList = new ArrayList();
        this.mTempRect1 = new Rect();
        this.mTempRect2 = new Rect();
        this.mTempRect3 = new Rect();
        this.mTempIntPair = new int[2];
        this.mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CoordinatorLayout, defStyleAttr, R.style.Widget_Design_CoordinatorLayout);
        int keylineArrayRes = a.getResourceId(R.styleable.CoordinatorLayout_keylines, 0);
        if (keylineArrayRes != 0) {
            Resources res = context.getResources();
            this.mKeylines = res.getIntArray(keylineArrayRes);
            float density = res.getDisplayMetrics().density;
            int count = this.mKeylines.length;
            for (int i = 0; i < count; i++) {
                int[] iArr = this.mKeylines;
                iArr[i] = (int) (((float) iArr[i]) * density);
            }
        }
        this.mStatusBarBackground = a.getDrawable(R.styleable.CoordinatorLayout_statusBarBackground);
        a.recycle();
        if (INSETS_HELPER != null) {
            INSETS_HELPER.setupForWindowInsets(this, new ApplyInsetsListener());
        }
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        resetTouchBehaviors();
        if (this.mNeedsPreDrawListener) {
            if (this.mOnPreDrawListener == null) {
                this.mOnPreDrawListener = new OnPreDrawListener();
            }
            getViewTreeObserver().addOnPreDrawListener(this.mOnPreDrawListener);
        }
        this.mIsAttachedToWindow = true;
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        resetTouchBehaviors();
        if (this.mNeedsPreDrawListener && this.mOnPreDrawListener != null) {
            getViewTreeObserver().removeOnPreDrawListener(this.mOnPreDrawListener);
        }
        if (this.mNestedScrollingTarget != null) {
            onStopNestedScroll(this.mNestedScrollingTarget);
        }
        this.mIsAttachedToWindow = false;
    }

    public void setStatusBarBackground(Drawable bg) {
        this.mStatusBarBackground = bg;
        invalidate();
    }

    public Drawable getStatusBarBackground() {
        return this.mStatusBarBackground;
    }

    public void setStatusBarBackgroundResource(int resId) {
        setStatusBarBackground(resId != 0 ? ContextCompat.getDrawable(getContext(), resId) : null);
    }

    public void setStatusBarBackgroundColor(int color) {
        setStatusBarBackground(new ColorDrawable(color));
    }

    /* access modifiers changed from: private */
    public void setWindowInsets(WindowInsetsCompat insets) {
        boolean z = true;
        if (this.mLastInsets != insets) {
            this.mLastInsets = insets;
            this.mDrawStatusBarBackground = insets != null && insets.getSystemWindowInsetTop() > 0;
            if (this.mDrawStatusBarBackground || getBackground() != null) {
                z = false;
            }
            setWillNotDraw(z);
            dispatchChildApplyWindowInsets(insets);
            requestLayout();
        }
    }

    private void resetTouchBehaviors() {
        if (this.mBehaviorTouchView != null) {
            Behavior b = ((LayoutParams) this.mBehaviorTouchView.getLayoutParams()).getBehavior();
            if (b != null) {
                long now = SystemClock.uptimeMillis();
                MotionEvent cancelEvent = MotionEvent.obtain(now, now, 3, 0.0f, 0.0f, 0);
                b.onTouchEvent(this, this.mBehaviorTouchView, cancelEvent);
                cancelEvent.recycle();
            }
            this.mBehaviorTouchView = null;
        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            ((LayoutParams) getChildAt(i).getLayoutParams()).resetTouchBehaviorTracking();
        }
    }

    private void getTopSortedChildren(List<View> out) {
        int childIndex;
        out.clear();
        boolean useCustomOrder = isChildrenDrawingOrderEnabled();
        int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            if (useCustomOrder) {
                childIndex = getChildDrawingOrder(childCount, i);
            } else {
                childIndex = i;
            }
            out.add(getChildAt(childIndex));
        }
        if (TOP_SORTED_CHILDREN_COMPARATOR != null) {
            Collections.sort(out, TOP_SORTED_CHILDREN_COMPARATOR);
        }
    }

    private boolean performIntercept(MotionEvent ev) {
        boolean intercepted = false;
        boolean newBlock = false;
        MotionEvent cancelEvent = null;
        int action = ev.getActionMasked();
        List<View> topmostChildList = this.mTempList1;
        getTopSortedChildren(topmostChildList);
        int childCount = topmostChildList.size();
        for (int i = 0; i < childCount; i++) {
            View child = topmostChildList.get(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            Behavior b = lp.getBehavior();
            if ((!intercepted && !newBlock) || action == 0) {
                if (!intercepted && b != null && (intercepted = b.onInterceptTouchEvent(this, child, ev))) {
                    this.mBehaviorTouchView = child;
                }
                boolean wasBlocking = lp.didBlockInteraction();
                boolean isBlocking = lp.isBlockingInteractionBelow(this, child);
                newBlock = isBlocking && !wasBlocking;
                if (isBlocking && !newBlock) {
                    break;
                }
            } else if (b != null) {
                if (cancelEvent != null) {
                    long now = SystemClock.uptimeMillis();
                    cancelEvent = MotionEvent.obtain(now, now, 3, 0.0f, 0.0f, 0);
                }
                b.onInterceptTouchEvent(this, child, cancelEvent);
            }
        }
        topmostChildList.clear();
        return intercepted;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        MotionEvent cancelEvent = null;
        int action = ev.getActionMasked();
        if (action == 0) {
            resetTouchBehaviors();
        }
        boolean intercepted = performIntercept(ev);
        if (cancelEvent != null) {
            cancelEvent.recycle();
        }
        if (action == 1 || action == 3) {
            resetTouchBehaviors();
        }
        return intercepted;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:2:0x000d, code lost:
        r11 = performIntercept(r15);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onTouchEvent(android.view.MotionEvent r15) {
        /*
            r14 = this;
            r4 = 3
            r5 = 0
            r12 = 0
            r11 = 0
            r10 = 0
            int r8 = r15.getActionMasked()
            android.view.View r2 = r14.mBehaviorTouchView
            if (r2 != 0) goto L_0x0013
            boolean r11 = r14.performIntercept(r15)
            if (r11 == 0) goto L_0x0026
        L_0x0013:
            android.view.View r2 = r14.mBehaviorTouchView
            android.view.ViewGroup$LayoutParams r13 = r2.getLayoutParams()
            android.support.design.widget.CoordinatorLayout$LayoutParams r13 = (android.support.design.widget.CoordinatorLayout.LayoutParams) r13
            android.support.design.widget.CoordinatorLayout$Behavior r9 = r13.getBehavior()
            if (r9 == 0) goto L_0x0026
            android.view.View r2 = r14.mBehaviorTouchView
            r9.onTouchEvent(r14, r2, r15)
        L_0x0026:
            android.view.View r2 = r14.mBehaviorTouchView
            if (r2 != 0) goto L_0x0041
            boolean r2 = super.onTouchEvent(r15)
            r12 = r12 | r2
        L_0x002f:
            if (r12 != 0) goto L_0x0033
            if (r8 != 0) goto L_0x0033
        L_0x0033:
            if (r10 == 0) goto L_0x0038
            r10.recycle()
        L_0x0038:
            r2 = 1
            if (r8 == r2) goto L_0x003d
            if (r8 != r4) goto L_0x0040
        L_0x003d:
            r14.resetTouchBehaviors()
        L_0x0040:
            return r12
        L_0x0041:
            if (r11 == 0) goto L_0x002f
            if (r10 == 0) goto L_0x0050
            long r0 = android.os.SystemClock.uptimeMillis()
            r7 = 0
            r2 = r0
            r6 = r5
            android.view.MotionEvent r10 = android.view.MotionEvent.obtain(r0, r2, r4, r5, r6, r7)
        L_0x0050:
            super.onTouchEvent(r10)
            goto L_0x002f
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.design.widget.CoordinatorLayout.onTouchEvent(android.view.MotionEvent):boolean");
    }

    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
        if (disallowIntercept) {
            resetTouchBehaviors();
        }
    }

    private int getKeyline(int index) {
        if (this.mKeylines == null) {
            Log.e(TAG, "No keylines defined for " + this + " - attempted index lookup " + index);
            return 0;
        } else if (index >= 0 && index < this.mKeylines.length) {
            return this.mKeylines[index];
        } else {
            Log.e(TAG, "Keyline index " + index + " out of range for " + this);
            return 0;
        }
    }

    static Behavior parseBehavior(Context context, AttributeSet attrs, String name) {
        String fullName;
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        if (name.startsWith(".")) {
            fullName = context.getPackageName() + name;
        } else if (name.indexOf(46) >= 0) {
            fullName = name;
        } else {
            fullName = WIDGET_PACKAGE_NAME + '.' + name;
        }
        try {
            Map<String, Constructor<Behavior>> constructors = sConstructors.get();
            if (constructors == null) {
                constructors = new HashMap<>();
                sConstructors.set(constructors);
            }
            Constructor<?> constructor = constructors.get(fullName);
            if (constructor == null) {
                constructor = Class.forName(fullName, true, context.getClassLoader()).getConstructor(CONSTRUCTOR_PARAMS);
                constructors.put(fullName, constructor);
            }
            return (Behavior) constructor.newInstance(new Object[]{context, attrs});
        } catch (Exception e) {
            throw new RuntimeException("Could not inflate Behavior subclass " + fullName, e);
        }
    }

    /* access modifiers changed from: package-private */
    public LayoutParams getResolvedLayoutParams(View child) {
        LayoutParams result = (LayoutParams) child.getLayoutParams();
        if (!result.mBehaviorResolved) {
            DefaultBehavior defaultBehavior = null;
            for (Class cls = child.getClass(); cls != null; cls = cls.getSuperclass()) {
                defaultBehavior = (DefaultBehavior) cls.getAnnotation(DefaultBehavior.class);
                if (defaultBehavior != null) {
                    break;
                }
            }
            if (defaultBehavior != null) {
                try {
                    result.setBehavior((Behavior) defaultBehavior.value().newInstance());
                } catch (Exception e) {
                    Log.e(TAG, "Default behavior class " + defaultBehavior.value().getName() + " could not be instantiated. Did you forget a default constructor?", e);
                }
            }
            result.mBehaviorResolved = true;
        }
        return result;
    }

    private void prepareChildren() {
        int childCount = getChildCount();
        boolean resortRequired = this.mDependencySortedChildren.size() != childCount;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            LayoutParams lp = getResolvedLayoutParams(child);
            if (!resortRequired && lp.isDirty(this, child)) {
                resortRequired = true;
            }
            lp.findAnchorView(this, child);
        }
        if (resortRequired) {
            this.mDependencySortedChildren.clear();
            for (int i2 = 0; i2 < childCount; i2++) {
                this.mDependencySortedChildren.add(getChildAt(i2));
            }
            Collections.sort(this.mDependencySortedChildren, this.mLayoutDependencyComparator);
        }
    }

    /* access modifiers changed from: package-private */
    public void getDescendantRect(View descendant, Rect out) {
        ViewGroupUtils.getDescendantRect(this, descendant, out);
    }

    /* access modifiers changed from: protected */
    public int getSuggestedMinimumWidth() {
        return Math.max(super.getSuggestedMinimumWidth(), getPaddingLeft() + getPaddingRight());
    }

    /* access modifiers changed from: protected */
    public int getSuggestedMinimumHeight() {
        return Math.max(super.getSuggestedMinimumHeight(), getPaddingTop() + getPaddingBottom());
    }

    public void onMeasureChild(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        measureChildWithMargins(child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        prepareChildren();
        ensurePreDrawListener();
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        boolean isRtl = layoutDirection == 1;
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int widthPadding = paddingLeft + paddingRight;
        int heightPadding = paddingTop + paddingBottom;
        int widthUsed = getSuggestedMinimumWidth();
        int heightUsed = getSuggestedMinimumHeight();
        int childState = 0;
        boolean applyInsets = this.mLastInsets != null && ViewCompat.getFitsSystemWindows(this);
        int childCount = this.mDependencySortedChildren.size();
        for (int i = 0; i < childCount; i++) {
            View child = this.mDependencySortedChildren.get(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int keylineWidthUsed = 0;
            if (lp.keyline >= 0 && widthMode != 0) {
                int keylinePos = getKeyline(lp.keyline);
                int keylineGravity = GravityCompat.getAbsoluteGravity(resolveKeylineGravity(lp.gravity), layoutDirection) & 7;
                if ((keylineGravity == 3 && !isRtl) || (keylineGravity == 5 && isRtl)) {
                    keylineWidthUsed = Math.max(0, (widthSize - paddingRight) - keylinePos);
                } else if ((keylineGravity == 5 && !isRtl) || (keylineGravity == 3 && isRtl)) {
                    keylineWidthUsed = Math.max(0, keylinePos - paddingLeft);
                }
            }
            int childWidthMeasureSpec = widthMeasureSpec;
            int childHeightMeasureSpec = heightMeasureSpec;
            if (applyInsets && !ViewCompat.getFitsSystemWindows(child)) {
                int horizInsets = this.mLastInsets.getSystemWindowInsetLeft() + this.mLastInsets.getSystemWindowInsetRight();
                int vertInsets = this.mLastInsets.getSystemWindowInsetTop() + this.mLastInsets.getSystemWindowInsetBottom();
                childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(widthSize - horizInsets, widthMode);
                childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(heightSize - vertInsets, heightMode);
            }
            Behavior b = lp.getBehavior();
            if (b == null || !b.onMeasureChild(this, child, childWidthMeasureSpec, keylineWidthUsed, childHeightMeasureSpec, 0)) {
                onMeasureChild(child, childWidthMeasureSpec, keylineWidthUsed, childHeightMeasureSpec, 0);
            }
            widthUsed = Math.max(widthUsed, child.getMeasuredWidth() + widthPadding + lp.leftMargin + lp.rightMargin);
            heightUsed = Math.max(heightUsed, child.getMeasuredHeight() + heightPadding + lp.topMargin + lp.bottomMargin);
            childState = ViewCompat.combineMeasuredStates(childState, ViewCompat.getMeasuredState(child));
        }
        setMeasuredDimension(ViewCompat.resolveSizeAndState(widthUsed, widthMeasureSpec, -16777216 & childState), ViewCompat.resolveSizeAndState(heightUsed, heightMeasureSpec, childState << 16));
    }

    private void dispatchChildApplyWindowInsets(WindowInsetsCompat insets) {
        if (!insets.isConsumed()) {
            int z = getChildCount();
            for (int i = 0; i < z; i++) {
                View child = getChildAt(i);
                if (ViewCompat.getFitsSystemWindows(child)) {
                    Behavior b = ((LayoutParams) child.getLayoutParams()).getBehavior();
                    if (b != null) {
                        insets = b.onApplyWindowInsets(this, child, insets);
                        if (insets.isConsumed()) {
                            return;
                        }
                    }
                    insets = ViewCompat.dispatchApplyWindowInsets(child, insets);
                    if (insets.isConsumed()) {
                        return;
                    }
                }
            }
        }
    }

    public void onLayoutChild(View child, int layoutDirection) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.checkAnchorChanged()) {
            throw new IllegalStateException("An anchor may not be changed after CoordinatorLayout measurement begins before layout is complete.");
        } else if (lp.mAnchorView != null) {
            layoutChildWithAnchor(child, lp.mAnchorView, layoutDirection);
        } else if (lp.keyline >= 0) {
            layoutChildWithKeyline(child, lp.keyline, layoutDirection);
        } else {
            layoutChild(child, layoutDirection);
        }
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        int childCount = this.mDependencySortedChildren.size();
        for (int i = 0; i < childCount; i++) {
            View child = this.mDependencySortedChildren.get(i);
            Behavior behavior = ((LayoutParams) child.getLayoutParams()).getBehavior();
            if (behavior == null || !behavior.onLayoutChild(this, child, layoutDirection)) {
                onLayoutChild(child, layoutDirection);
            }
        }
    }

    public void onDraw(Canvas c) {
        super.onDraw(c);
        if (this.mDrawStatusBarBackground && this.mStatusBarBackground != null) {
            int inset = this.mLastInsets != null ? this.mLastInsets.getSystemWindowInsetTop() : 0;
            if (inset > 0) {
                this.mStatusBarBackground.setBounds(0, 0, getWidth(), inset);
                this.mStatusBarBackground.draw(c);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void recordLastChildRect(View child, Rect r) {
        ((LayoutParams) child.getLayoutParams()).setLastChildRect(r);
    }

    /* access modifiers changed from: package-private */
    public void getLastChildRect(View child, Rect out) {
        out.set(((LayoutParams) child.getLayoutParams()).getLastChildRect());
    }

    /* access modifiers changed from: package-private */
    public void getChildRect(View child, boolean transform, Rect out) {
        if (child.isLayoutRequested() || child.getVisibility() == 8) {
            out.set(0, 0, 0, 0);
        } else if (transform) {
            getDescendantRect(child, out);
        } else {
            out.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
        }
    }

    /* access modifiers changed from: package-private */
    public void getDesiredAnchoredChildRect(View child, int layoutDirection, Rect anchorRect, Rect out) {
        int left;
        int top;
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int absGravity = GravityCompat.getAbsoluteGravity(resolveAnchoredChildGravity(lp.gravity), layoutDirection);
        int absAnchorGravity = GravityCompat.getAbsoluteGravity(resolveGravity(lp.anchorGravity), layoutDirection);
        int hgrav = absGravity & 7;
        int vgrav = absGravity & Constants.BILLING_ERROR_SKUDETAILS_FAILED;
        int anchorHgrav = absAnchorGravity & 7;
        int anchorVgrav = absAnchorGravity & Constants.BILLING_ERROR_SKUDETAILS_FAILED;
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        switch (anchorHgrav) {
            case 1:
                left = anchorRect.left + (anchorRect.width() / 2);
                break;
            case 5:
                left = anchorRect.right;
                break;
            default:
                left = anchorRect.left;
                break;
        }
        switch (anchorVgrav) {
            case 16:
                top = anchorRect.top + (anchorRect.height() / 2);
                break;
            case 80:
                top = anchorRect.bottom;
                break;
            default:
                top = anchorRect.top;
                break;
        }
        switch (hgrav) {
            case 1:
                left -= childWidth / 2;
                break;
            case 5:
                break;
            default:
                left -= childWidth;
                break;
        }
        switch (vgrav) {
            case 16:
                top -= childHeight / 2;
                break;
            case 80:
                break;
            default:
                top -= childHeight;
                break;
        }
        int width = getWidth();
        int height = getHeight();
        int left2 = Math.max(getPaddingLeft() + lp.leftMargin, Math.min(left, ((width - getPaddingRight()) - childWidth) - lp.rightMargin));
        int top2 = Math.max(getPaddingTop() + lp.topMargin, Math.min(top, ((height - getPaddingBottom()) - childHeight) - lp.bottomMargin));
        out.set(left2, top2, left2 + childWidth, top2 + childHeight);
    }

    private void layoutChildWithAnchor(View child, View anchor, int layoutDirection) {
        LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
        Rect anchorRect = this.mTempRect1;
        Rect childRect = this.mTempRect2;
        getDescendantRect(anchor, anchorRect);
        getDesiredAnchoredChildRect(child, layoutDirection, anchorRect, childRect);
        child.layout(childRect.left, childRect.top, childRect.right, childRect.bottom);
    }

    private void layoutChildWithKeyline(View child, int keyline, int layoutDirection) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int absGravity = GravityCompat.getAbsoluteGravity(resolveKeylineGravity(lp.gravity), layoutDirection);
        int hgrav = absGravity & 7;
        int vgrav = absGravity & Constants.BILLING_ERROR_SKUDETAILS_FAILED;
        int width = getWidth();
        int height = getHeight();
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        if (layoutDirection == 1) {
            keyline = width - keyline;
        }
        int left = getKeyline(keyline) - childWidth;
        int top = 0;
        switch (hgrav) {
            case 1:
                left += childWidth / 2;
                break;
            case 5:
                left += childWidth;
                break;
        }
        switch (vgrav) {
            case 16:
                top = 0 + (childHeight / 2);
                break;
            case 80:
                top = 0 + childHeight;
                break;
        }
        int left2 = Math.max(getPaddingLeft() + lp.leftMargin, Math.min(left, ((width - getPaddingRight()) - childWidth) - lp.rightMargin));
        int top2 = Math.max(getPaddingTop() + lp.topMargin, Math.min(top, ((height - getPaddingBottom()) - childHeight) - lp.bottomMargin));
        child.layout(left2, top2, left2 + childWidth, top2 + childHeight);
    }

    private void layoutChild(View child, int layoutDirection) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        Rect parent = this.mTempRect1;
        parent.set(getPaddingLeft() + lp.leftMargin, getPaddingTop() + lp.topMargin, (getWidth() - getPaddingRight()) - lp.rightMargin, (getHeight() - getPaddingBottom()) - lp.bottomMargin);
        if (this.mLastInsets != null && ViewCompat.getFitsSystemWindows(this) && !ViewCompat.getFitsSystemWindows(child)) {
            parent.left += this.mLastInsets.getSystemWindowInsetLeft();
            parent.top += this.mLastInsets.getSystemWindowInsetTop();
            parent.right -= this.mLastInsets.getSystemWindowInsetRight();
            parent.bottom -= this.mLastInsets.getSystemWindowInsetBottom();
        }
        Rect out = this.mTempRect2;
        GravityCompat.apply(resolveGravity(lp.gravity), child.getMeasuredWidth(), child.getMeasuredHeight(), parent, out, layoutDirection);
        child.layout(out.left, out.top, out.right, out.bottom);
    }

    private static int resolveGravity(int gravity) {
        if (gravity == 0) {
            return 8388659;
        }
        return gravity;
    }

    private static int resolveKeylineGravity(int gravity) {
        if (gravity == 0) {
            return 8388661;
        }
        return gravity;
    }

    private static int resolveAnchoredChildGravity(int gravity) {
        if (gravity == 0) {
            return 17;
        }
        return gravity;
    }

    /* access modifiers changed from: protected */
    public boolean drawChild(Canvas canvas, View child, long drawingTime) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.mBehavior != null && lp.mBehavior.getScrimOpacity(this, child) > 0.0f) {
            if (this.mScrimPaint == null) {
                this.mScrimPaint = new Paint();
            }
            this.mScrimPaint.setColor(lp.mBehavior.getScrimColor(this, child));
            canvas.drawRect((float) getPaddingLeft(), (float) getPaddingTop(), (float) (getWidth() - getPaddingRight()), (float) (getHeight() - getPaddingBottom()), this.mScrimPaint);
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    /* access modifiers changed from: package-private */
    public void dispatchOnDependentViewChanged(boolean fromNestedScroll) {
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        int childCount = this.mDependencySortedChildren.size();
        for (int i = 0; i < childCount; i++) {
            View child = this.mDependencySortedChildren.get(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            for (int j = 0; j < i; j++) {
                if (lp.mAnchorDirectChild == this.mDependencySortedChildren.get(j)) {
                    offsetChildToAnchor(child, layoutDirection);
                }
            }
            Rect oldRect = this.mTempRect1;
            Rect newRect = this.mTempRect2;
            getLastChildRect(child, oldRect);
            getChildRect(child, true, newRect);
            if (!oldRect.equals(newRect)) {
                recordLastChildRect(child, newRect);
                for (int j2 = i + 1; j2 < childCount; j2++) {
                    View checkChild = this.mDependencySortedChildren.get(j2);
                    LayoutParams checkLp = (LayoutParams) checkChild.getLayoutParams();
                    Behavior b = checkLp.getBehavior();
                    if (b != null && b.layoutDependsOn(this, checkChild, child)) {
                        if (fromNestedScroll || !checkLp.getChangedAfterNestedScroll()) {
                            boolean handled = b.onDependentViewChanged(this, checkChild, child);
                            if (fromNestedScroll) {
                                checkLp.setChangedAfterNestedScroll(handled);
                            }
                        } else {
                            checkLp.resetChangedAfterNestedScroll();
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:7:0x001a, code lost:
        r4 = (android.support.design.widget.CoordinatorLayout.LayoutParams) r1.getLayoutParams();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void dispatchDependentViewsChanged(android.view.View r8) {
        /*
            r7 = this;
            java.util.List<android.view.View> r6 = r7.mDependencySortedChildren
            int r2 = r6.size()
            r5 = 0
            r3 = 0
        L_0x0008:
            if (r3 >= r2) goto L_0x0030
            java.util.List<android.view.View> r6 = r7.mDependencySortedChildren
            java.lang.Object r1 = r6.get(r3)
            android.view.View r1 = (android.view.View) r1
            if (r1 != r8) goto L_0x0018
            r5 = 1
        L_0x0015:
            int r3 = r3 + 1
            goto L_0x0008
        L_0x0018:
            if (r5 == 0) goto L_0x0015
            android.view.ViewGroup$LayoutParams r4 = r1.getLayoutParams()
            android.support.design.widget.CoordinatorLayout$LayoutParams r4 = (android.support.design.widget.CoordinatorLayout.LayoutParams) r4
            android.support.design.widget.CoordinatorLayout$Behavior r0 = r4.getBehavior()
            if (r0 == 0) goto L_0x0015
            boolean r6 = r4.dependsOn(r7, r1, r8)
            if (r6 == 0) goto L_0x0015
            r0.onDependentViewChanged(r7, r1, r8)
            goto L_0x0015
        L_0x0030:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.design.widget.CoordinatorLayout.dispatchDependentViewsChanged(android.view.View):void");
    }

    public List<View> getDependencies(View child) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        List<View> list = this.mTempDependenciesList;
        list.clear();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View other = getChildAt(i);
            if (other != child && lp.dependsOn(this, child, other)) {
                list.add(other);
            }
        }
        return list;
    }

    /* access modifiers changed from: package-private */
    public void ensurePreDrawListener() {
        boolean hasDependencies = false;
        int childCount = getChildCount();
        int i = 0;
        while (true) {
            if (i >= childCount) {
                break;
            } else if (hasDependencies(getChildAt(i))) {
                hasDependencies = true;
                break;
            } else {
                i++;
            }
        }
        if (hasDependencies == this.mNeedsPreDrawListener) {
            return;
        }
        if (hasDependencies) {
            addPreDrawListener();
        } else {
            removePreDrawListener();
        }
    }

    /* access modifiers changed from: package-private */
    public boolean hasDependencies(View child) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.mAnchorView != null) {
            return true;
        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View other = getChildAt(i);
            if (other != child && lp.dependsOn(this, child, other)) {
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public void addPreDrawListener() {
        if (this.mIsAttachedToWindow) {
            if (this.mOnPreDrawListener == null) {
                this.mOnPreDrawListener = new OnPreDrawListener();
            }
            getViewTreeObserver().addOnPreDrawListener(this.mOnPreDrawListener);
        }
        this.mNeedsPreDrawListener = true;
    }

    /* access modifiers changed from: package-private */
    public void removePreDrawListener() {
        if (this.mIsAttachedToWindow && this.mOnPreDrawListener != null) {
            getViewTreeObserver().removeOnPreDrawListener(this.mOnPreDrawListener);
        }
        this.mNeedsPreDrawListener = false;
    }

    /* access modifiers changed from: package-private */
    public void offsetChildToAnchor(View child, int layoutDirection) {
        Behavior b;
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.mAnchorView != null) {
            Rect anchorRect = this.mTempRect1;
            Rect childRect = this.mTempRect2;
            Rect desiredChildRect = this.mTempRect3;
            getDescendantRect(lp.mAnchorView, anchorRect);
            getChildRect(child, false, childRect);
            getDesiredAnchoredChildRect(child, layoutDirection, anchorRect, desiredChildRect);
            int dx = desiredChildRect.left - childRect.left;
            int dy = desiredChildRect.top - childRect.top;
            if (dx != 0) {
                child.offsetLeftAndRight(dx);
            }
            if (dy != 0) {
                child.offsetTopAndBottom(dy);
            }
            if ((dx != 0 || dy != 0) && (b = lp.getBehavior()) != null) {
                b.onDependentViewChanged(this, child, lp.mAnchorView);
            }
        }
    }

    public boolean isPointInChildBounds(View child, int x, int y) {
        Rect r = this.mTempRect1;
        getDescendantRect(child, r);
        return r.contains(x, y);
    }

    public boolean doViewsOverlap(View first, View second) {
        boolean z;
        boolean z2;
        if (first.getVisibility() != 0 || second.getVisibility() != 0) {
            return false;
        }
        Rect firstRect = this.mTempRect1;
        if (first.getParent() != this) {
            z = true;
        } else {
            z = false;
        }
        getChildRect(first, z, firstRect);
        Rect secondRect = this.mTempRect2;
        if (second.getParent() != this) {
            z2 = true;
        } else {
            z2 = false;
        }
        getChildRect(second, z2, secondRect);
        if (firstRect.left > secondRect.right || firstRect.top > secondRect.bottom || firstRect.right < secondRect.left || firstRect.bottom < secondRect.top) {
            return false;
        }
        return true;
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /* access modifiers changed from: protected */
    public LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (p instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) p);
        }
        if (p instanceof ViewGroup.MarginLayoutParams) {
            return new LayoutParams((ViewGroup.MarginLayoutParams) p);
        }
        return new LayoutParams(p);
    }

    /* access modifiers changed from: protected */
    public LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    /* access modifiers changed from: protected */
    public boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return (p instanceof LayoutParams) && super.checkLayoutParams(p);
    }

    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        boolean handled = false;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            Behavior viewBehavior = lp.getBehavior();
            if (viewBehavior != null) {
                boolean accepted = viewBehavior.onStartNestedScroll(this, view, child, target, nestedScrollAxes);
                handled |= accepted;
                lp.acceptNestedScroll(accepted);
            } else {
                lp.acceptNestedScroll(false);
            }
        }
        return handled;
    }

    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
        Behavior viewBehavior;
        this.mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, nestedScrollAxes);
        this.mNestedScrollingDirectChild = child;
        this.mNestedScrollingTarget = target;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp.isNestedScrollAccepted() && (viewBehavior = lp.getBehavior()) != null) {
                viewBehavior.onNestedScrollAccepted(this, view, child, target, nestedScrollAxes);
            }
        }
    }

    public void onStopNestedScroll(View target) {
        this.mNestedScrollingParentHelper.onStopNestedScroll(target);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp.isNestedScrollAccepted()) {
                Behavior viewBehavior = lp.getBehavior();
                if (viewBehavior != null) {
                    viewBehavior.onStopNestedScroll(this, view, target);
                }
                lp.resetNestedScroll();
                lp.resetChangedAfterNestedScroll();
            }
        }
        this.mNestedScrollingDirectChild = null;
        this.mNestedScrollingTarget = null;
    }

    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        Behavior viewBehavior;
        int childCount = getChildCount();
        boolean accepted = false;
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp.isNestedScrollAccepted() && (viewBehavior = lp.getBehavior()) != null) {
                viewBehavior.onNestedScroll(this, view, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
                accepted = true;
            }
        }
        if (accepted) {
            dispatchOnDependentViewChanged(true);
        }
    }

    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        Behavior viewBehavior;
        int xConsumed = 0;
        int yConsumed = 0;
        boolean accepted = false;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp.isNestedScrollAccepted() && (viewBehavior = lp.getBehavior()) != null) {
                int[] iArr = this.mTempIntPair;
                this.mTempIntPair[1] = 0;
                iArr[0] = 0;
                viewBehavior.onNestedPreScroll(this, view, target, dx, dy, this.mTempIntPair);
                xConsumed = dx > 0 ? Math.max(xConsumed, this.mTempIntPair[0]) : Math.min(xConsumed, this.mTempIntPair[0]);
                yConsumed = dy > 0 ? Math.max(yConsumed, this.mTempIntPair[1]) : Math.min(yConsumed, this.mTempIntPair[1]);
                accepted = true;
            }
        }
        consumed[0] = xConsumed;
        consumed[1] = yConsumed;
        if (accepted) {
            dispatchOnDependentViewChanged(true);
        }
    }

    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        Behavior viewBehavior;
        boolean handled = false;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp.isNestedScrollAccepted() && (viewBehavior = lp.getBehavior()) != null) {
                handled |= viewBehavior.onNestedFling(this, view, target, velocityX, velocityY, consumed);
            }
        }
        if (handled) {
            dispatchOnDependentViewChanged(true);
        }
        return handled;
    }

    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        Behavior viewBehavior;
        boolean handled = false;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp.isNestedScrollAccepted() && (viewBehavior = lp.getBehavior()) != null) {
                handled |= viewBehavior.onNestedPreFling(this, view, target, velocityX, velocityY);
            }
        }
        return handled;
    }

    public int getNestedScrollAxes() {
        return this.mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    class OnPreDrawListener implements ViewTreeObserver.OnPreDrawListener {
        OnPreDrawListener() {
        }

        public boolean onPreDraw() {
            CoordinatorLayout.this.dispatchOnDependentViewChanged(false);
            return true;
        }
    }

    static class ViewElevationComparator implements Comparator<View> {
        ViewElevationComparator() {
        }

        public int compare(View lhs, View rhs) {
            float lz = ViewCompat.getZ(lhs);
            float rz = ViewCompat.getZ(rhs);
            if (lz > rz) {
                return -1;
            }
            if (lz < rz) {
                return 1;
            }
            return 0;
        }
    }

    public static abstract class Behavior<V extends View> {
        public Behavior() {
        }

        public Behavior(Context context, AttributeSet attrs) {
        }

        public boolean onInterceptTouchEvent(CoordinatorLayout parent, V v, MotionEvent ev) {
            return false;
        }

        public boolean onTouchEvent(CoordinatorLayout parent, V v, MotionEvent ev) {
            return false;
        }

        public final int getScrimColor(CoordinatorLayout parent, V v) {
            return ViewCompat.MEASURED_STATE_MASK;
        }

        public final float getScrimOpacity(CoordinatorLayout parent, V v) {
            return 0.0f;
        }

        public boolean blocksInteractionBelow(CoordinatorLayout parent, V child) {
            return getScrimOpacity(parent, child) > 0.0f;
        }

        public boolean layoutDependsOn(CoordinatorLayout parent, V v, View dependency) {
            return false;
        }

        public boolean onDependentViewChanged(CoordinatorLayout parent, V v, View dependency) {
            return false;
        }

        public boolean isDirty(CoordinatorLayout parent, V v) {
            return false;
        }

        public boolean onMeasureChild(CoordinatorLayout parent, V v, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
            return false;
        }

        public boolean onLayoutChild(CoordinatorLayout parent, V v, int layoutDirection) {
            return false;
        }

        public static void setTag(View child, Object tag) {
            ((LayoutParams) child.getLayoutParams()).mBehaviorTag = tag;
        }

        public static Object getTag(View child) {
            return ((LayoutParams) child.getLayoutParams()).mBehaviorTag;
        }

        public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, V v, View directTargetChild, View target, int nestedScrollAxes) {
            return false;
        }

        public void onNestedScrollAccepted(CoordinatorLayout coordinatorLayout, V v, View directTargetChild, View target, int nestedScrollAxes) {
        }

        public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, V v, View target) {
        }

        public void onNestedScroll(CoordinatorLayout coordinatorLayout, V v, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        }

        public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, V v, View target, int dx, int dy, int[] consumed) {
        }

        public boolean onNestedFling(CoordinatorLayout coordinatorLayout, V v, View target, float velocityX, float velocityY, boolean consumed) {
            return false;
        }

        public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, V v, View target, float velocityX, float velocityY) {
            return false;
        }

        public WindowInsetsCompat onApplyWindowInsets(CoordinatorLayout coordinatorLayout, V v, WindowInsetsCompat insets) {
            return insets;
        }

        public void onRestoreInstanceState(CoordinatorLayout parent, V v, Parcelable state) {
        }

        public Parcelable onSaveInstanceState(CoordinatorLayout parent, V v) {
            return View.BaseSavedState.EMPTY_STATE;
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public int anchorGravity = 0;
        public int gravity = 0;
        public int keyline = -1;
        View mAnchorDirectChild;
        int mAnchorId = -1;
        View mAnchorView;
        Behavior mBehavior;
        boolean mBehaviorResolved = false;
        Object mBehaviorTag;
        private boolean mDidAcceptNestedScroll;
        private boolean mDidBlockInteraction;
        private boolean mDidChangeAfterNestedScroll;
        final Rect mLastChildRect = new Rect();

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CoordinatorLayout_LayoutParams);
            this.gravity = a.getInteger(R.styleable.CoordinatorLayout_LayoutParams_android_layout_gravity, 0);
            this.mAnchorId = a.getResourceId(R.styleable.CoordinatorLayout_LayoutParams_layout_anchor, -1);
            this.anchorGravity = a.getInteger(R.styleable.CoordinatorLayout_LayoutParams_layout_anchorGravity, 0);
            this.keyline = a.getInteger(R.styleable.CoordinatorLayout_LayoutParams_layout_keyline, -1);
            this.mBehaviorResolved = a.hasValue(R.styleable.CoordinatorLayout_LayoutParams_layout_behavior);
            if (this.mBehaviorResolved) {
                this.mBehavior = CoordinatorLayout.parseBehavior(context, attrs, a.getString(R.styleable.CoordinatorLayout_LayoutParams_layout_behavior));
            }
            a.recycle();
        }

        public LayoutParams(LayoutParams p) {
            super(p);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams p) {
            super(p);
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
        }

        public int getAnchorId() {
            return this.mAnchorId;
        }

        public void setAnchorId(int id) {
            invalidateAnchor();
            this.mAnchorId = id;
        }

        public Behavior getBehavior() {
            return this.mBehavior;
        }

        public void setBehavior(Behavior behavior) {
            if (this.mBehavior != behavior) {
                this.mBehavior = behavior;
                this.mBehaviorTag = null;
                this.mBehaviorResolved = true;
            }
        }

        /* access modifiers changed from: package-private */
        public void setLastChildRect(Rect r) {
            this.mLastChildRect.set(r);
        }

        /* access modifiers changed from: package-private */
        public Rect getLastChildRect() {
            return this.mLastChildRect;
        }

        /* access modifiers changed from: package-private */
        public boolean checkAnchorChanged() {
            return this.mAnchorView == null && this.mAnchorId != -1;
        }

        /* access modifiers changed from: package-private */
        public boolean didBlockInteraction() {
            if (this.mBehavior == null) {
                this.mDidBlockInteraction = false;
            }
            return this.mDidBlockInteraction;
        }

        /* access modifiers changed from: package-private */
        public boolean isBlockingInteractionBelow(CoordinatorLayout parent, View child) {
            if (this.mDidBlockInteraction) {
                return true;
            }
            boolean blocksInteractionBelow = (this.mBehavior != null ? this.mBehavior.blocksInteractionBelow(parent, child) : false) | this.mDidBlockInteraction;
            this.mDidBlockInteraction = blocksInteractionBelow;
            return blocksInteractionBelow;
        }

        /* access modifiers changed from: package-private */
        public void resetTouchBehaviorTracking() {
            this.mDidBlockInteraction = false;
        }

        /* access modifiers changed from: package-private */
        public void resetNestedScroll() {
            this.mDidAcceptNestedScroll = false;
        }

        /* access modifiers changed from: package-private */
        public void acceptNestedScroll(boolean accept) {
            this.mDidAcceptNestedScroll = accept;
        }

        /* access modifiers changed from: package-private */
        public boolean isNestedScrollAccepted() {
            return this.mDidAcceptNestedScroll;
        }

        /* access modifiers changed from: package-private */
        public boolean getChangedAfterNestedScroll() {
            return this.mDidChangeAfterNestedScroll;
        }

        /* access modifiers changed from: package-private */
        public void setChangedAfterNestedScroll(boolean changed) {
            this.mDidChangeAfterNestedScroll = changed;
        }

        /* access modifiers changed from: package-private */
        public void resetChangedAfterNestedScroll() {
            this.mDidChangeAfterNestedScroll = false;
        }

        /* access modifiers changed from: package-private */
        public boolean dependsOn(CoordinatorLayout parent, View child, View dependency) {
            return dependency == this.mAnchorDirectChild || (this.mBehavior != null && this.mBehavior.layoutDependsOn(parent, child, dependency));
        }

        /* access modifiers changed from: package-private */
        public void invalidateAnchor() {
            this.mAnchorDirectChild = null;
            this.mAnchorView = null;
        }

        /* access modifiers changed from: package-private */
        public View findAnchorView(CoordinatorLayout parent, View forChild) {
            if (this.mAnchorId == -1) {
                this.mAnchorDirectChild = null;
                this.mAnchorView = null;
                return null;
            }
            if (this.mAnchorView == null || !verifyAnchorView(forChild, parent)) {
                resolveAnchorView(forChild, parent);
            }
            return this.mAnchorView;
        }

        /* access modifiers changed from: package-private */
        public boolean isDirty(CoordinatorLayout parent, View child) {
            return this.mBehavior != null && this.mBehavior.isDirty(parent, child);
        }

        private void resolveAnchorView(View forChild, CoordinatorLayout parent) {
            this.mAnchorView = parent.findViewById(this.mAnchorId);
            if (this.mAnchorView != null) {
                View directChild = this.mAnchorView;
                ViewParent p = this.mAnchorView.getParent();
                while (p != parent && p != null) {
                    if (p != forChild) {
                        if (p instanceof View) {
                            directChild = (View) p;
                        }
                        p = p.getParent();
                    } else if (parent.isInEditMode()) {
                        this.mAnchorDirectChild = null;
                        this.mAnchorView = null;
                        return;
                    } else {
                        throw new IllegalStateException("Anchor must not be a descendant of the anchored view");
                    }
                }
                this.mAnchorDirectChild = directChild;
            } else if (parent.isInEditMode()) {
                this.mAnchorDirectChild = null;
                this.mAnchorView = null;
            } else {
                throw new IllegalStateException("Could not find CoordinatorLayout descendant view with id " + parent.getResources().getResourceName(this.mAnchorId) + " to anchor view " + forChild);
            }
        }

        private boolean verifyAnchorView(View forChild, CoordinatorLayout parent) {
            if (this.mAnchorView.getId() != this.mAnchorId) {
                return false;
            }
            View directChild = this.mAnchorView;
            for (ViewParent p = this.mAnchorView.getParent(); p != parent; p = p.getParent()) {
                if (p == null || p == forChild) {
                    this.mAnchorDirectChild = null;
                    this.mAnchorView = null;
                    return false;
                }
                if (p instanceof View) {
                    directChild = (View) p;
                }
            }
            this.mAnchorDirectChild = directChild;
            return true;
        }
    }

    final class ApplyInsetsListener implements OnApplyWindowInsetsListener {
        ApplyInsetsListener() {
        }

        public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
            CoordinatorLayout.this.setWindowInsets(insets);
            return insets.consumeSystemWindowInsets();
        }
    }

    /* access modifiers changed from: protected */
    public void onRestoreInstanceState(Parcelable state) {
        Parcelable savedState;
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        SparseArray<Parcelable> behaviorStates = ss.behaviorStates;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int childId = child.getId();
            Behavior b = getResolvedLayoutParams(child).getBehavior();
            if (!(childId == -1 || b == null || (savedState = behaviorStates.get(childId)) == null)) {
                b.onRestoreInstanceState(this, child, savedState);
            }
        }
    }

    /* access modifiers changed from: protected */
    public Parcelable onSaveInstanceState() {
        Parcelable state;
        SavedState ss = new SavedState(super.onSaveInstanceState());
        SparseArray<Parcelable> behaviorStates = new SparseArray<>();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int childId = child.getId();
            Behavior b = ((LayoutParams) child.getLayoutParams()).getBehavior();
            if (!(childId == -1 || b == null || (state = b.onSaveInstanceState(this, child)) == null)) {
                behaviorStates.append(childId, state);
            }
        }
        ss.behaviorStates = behaviorStates;
        return ss;
    }

    protected static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        SparseArray<Parcelable> behaviorStates;

        public SavedState(Parcel source) {
            super(source);
            int size = source.readInt();
            int[] ids = new int[size];
            source.readIntArray(ids);
            Parcelable[] states = source.readParcelableArray(CoordinatorLayout.class.getClassLoader());
            this.behaviorStates = new SparseArray<>(size);
            for (int i = 0; i < size; i++) {
                this.behaviorStates.append(ids[i], states[i]);
            }
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            int size = this.behaviorStates != null ? this.behaviorStates.size() : 0;
            dest.writeInt(size);
            int[] ids = new int[size];
            Parcelable[] states = new Parcelable[size];
            for (int i = 0; i < size; i++) {
                ids[i] = this.behaviorStates.keyAt(i);
                states[i] = this.behaviorStates.valueAt(i);
            }
            dest.writeIntArray(ids);
            dest.writeParcelableArray(states, flags);
        }
    }
}
