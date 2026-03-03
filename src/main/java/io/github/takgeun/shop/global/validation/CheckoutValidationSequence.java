package io.github.takgeun.shop.global.validation;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;

@GroupSequence({
        ValidationGroups.Required.class,
        ValidationGroups.Format.class,
        Default.class
})
public interface CheckoutValidationSequence {
}
