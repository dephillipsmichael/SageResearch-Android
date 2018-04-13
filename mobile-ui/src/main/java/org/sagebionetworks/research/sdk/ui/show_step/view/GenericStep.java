/*
 * BSD 3-Clause License
 *
 * Copyright 2018  Sage Bionetworks. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1.  Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2.  Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * 3.  Neither the name of the copyright holder(s) nor the names of any contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission. No license is granted to the trademarks of
 * the copyright holders even if such marks are included in this software.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.sagebionetworks.research.sdk.ui.show_step.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.sagebionetworks.research.sdk.mobile.ui.R;
import org.sagebionetworks.research.sdk.mobile.ui.R2;
import org.sagebionetworks.research.sdk.ui.show_step.ShowStepContract.View;
import org.sagebionetworks.research.sdk.ui.show_step.StepPresenter;
import org.sagebionetworks.research.sdk.ui.widget.NavigationActionBar;

import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class GenericStep extends FrameLayout implements View<StepPresenter> {

    public static class Binding {
        @BindView(R2.id.description)
        public TextView description;

        @BindView(R2.id.rs2_step_navigation_action_bar)
        public NavigationActionBar navigationActionBar;

        @BindView(R2.id.title)
        public TextView title;
    }

    protected Binding binding;

    private LayoutInflater layoutInflater;

    private StepPresenter presenter;

    private Unbinder unbinder;

    public GenericStep(Context context) {
        super(context);
        init();
    }

    public GenericStep(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();

        TypedArray typedArray = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.GenericStep, 0, 0);
        initAttrs(typedArray);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public GenericStep(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();

        TypedArray typedArray = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.GenericStep, defStyleAttr, defStyleRes);
        initAttrs(typedArray);
    }

    @Override
    public void setPresenter(final StepPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void showActionButtons(@Nullable final Set actions) {

    }

    @LayoutRes
    protected int getLayoutId() {
        return R.layout.rs2_generic_step;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unbinder.unbind();
        presenter.detachView();
    }

    private void init() {
        layoutInflater = LayoutInflater.from(getContext());
        layoutInflater.inflate(getLayoutId(), this, true);

        binding = new Binding();
        unbinder = ButterKnife.bind(binding, this);

        binding.navigationActionBar
                .setActionButtonClickListener(ab -> presenter.handleAction(ab.getText().toString()));
    }

    private void initAttrs(TypedArray typedArray) {

    }
}