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

package org.sagebionetworks.research.presentation.model;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import org.sagebionetworks.research.domain.step.ui.theme.ColorPlacement;
import org.sagebionetworks.research.domain.step.ui.theme.ImageTheme;

@AutoValue
public abstract class ImageThemeView implements Parcelable {
    @AutoValue.Builder
    public abstract static class Builder {
        public abstract ImageThemeView build();

        public abstract Builder setColorPlacement(@Nullable

        @ColorPlacement String colorPlacement);

        public abstract Builder setImageResourceId(int imageResourceId);
    }

    /**
     * Creates an ImageThemeView from an ImageTheme.
     * @param imageTheme The image theme to create this imageThemeView from.
     * @return an ImageThemeView created from the given ImageTheme.
     */
    public static ImageThemeView fromImageTheme(@Nullable ImageTheme imageTheme) {
        return ImageThemeView.builder()
                .setColorPlacement(imageTheme.getColorPlacement())
                // TODO: rkolmos 05/29/2018 resolve the resource to get the image id here.
                .setImageResourceId(0)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_ImageThemeView.Builder();
    }

    @Nullable
    @ColorPlacement
    public abstract String getColorPlacement();

    public abstract int getImageResourceId();
}
