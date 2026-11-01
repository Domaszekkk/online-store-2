package com.estore.product;

import java.util.Set;
import java.util.UUID;

public class InvalidProductException extends ProductException {
    private InvalidProductException(String message) {
        super(message, 400);
    }

    public static InvalidProductException missingVariants() {
        return new InvalidProductException("Product must have at least one variant");
    }

    public static InvalidProductException duplicateStockKeepingUnit(String stockKeepingUnit) {
        return new InvalidProductException("Stock keeping unit " + stockKeepingUnit + " is used by more than one variant");
    }

    public static InvalidProductException electronicsWithOptions() {
        return new InvalidProductException("Product of type ELECTRONICS cannot have configurable options");
    }

    public static InvalidProductException electronicsWithManyVariants(int variantCount) {
        return new InvalidProductException("Product of type ELECTRONICS must have exactly one variant, but has " + variantCount);
    }

    public static InvalidProductException electronicsWithAddOns() {
        return new InvalidProductException("Product of type ELECTRONICS cannot have add-ons");
    }

    public static InvalidProductException duplicateOptionName(String optionName) {
        return new InvalidProductException("Option " + optionName + " is defined more than once");
    }

    public static InvalidProductException variantOptionsMismatch(String stockKeepingUnit, Set<String> optionNames) {
        return new InvalidProductException(
                "Variant " + stockKeepingUnit + " must have exactly one value for each option " + optionNames);
    }

    public static InvalidProductException unknownOptionValue(String stockKeepingUnit, String optionName, String value) {
        return new InvalidProductException(
                "Variant " + stockKeepingUnit + " has value " + value + " that is not allowed for option " + optionName);
    }

    public static InvalidProductException duplicateOptionCombination(String stockKeepingUnit) {
        return new InvalidProductException(
                "Variant " + stockKeepingUnit + " repeats the option values of another variant");
    }

    public static InvalidProductException unknownVariant(UUID variantId, UUID productId) {
        return new InvalidProductException("Variant " + variantId + " does not belong to product " + productId);
    }

    public static InvalidProductException usedAsAddOn(UUID productId, ProductType type) {
        return new InvalidProductException("Product " + productId + " is an add-on of other products, so its type cannot change to "
                + type + "; only ELECTRONICS products can be sold as add-ons");
    }

    public static InvalidProductException duplicateVariantId(UUID variantId) {
        return new InvalidProductException("Variant " + variantId + " is listed more than once");
    }
}
