package models.application

model = ApplicationModel
displayName = "Alternative view"

mappings = {
    Application {
        dynamicComponent {
            '[%subcomponents%]' {
                outSecondValue {
                    value "Application:dynamicComponent:[%subcomponents%]:outSecondValue:value"
                }
                outFirstValue {
                    value "Application:dynamicComponent:[%subcomponents%]:outFirstValue:value"
                }
            }
            outValue1 {
                value "Application:dynamicComponent:outValue1:value"
            }
        }
        composedComponent {
            subDynamicComponent {
                outValue1 {
                    value "Application:composedComponent:subDynamicComponent:outValue1:value"
                }
                '[%subcomponents%]' {
                    outFirstValue {
                        value "Application:composedComponent:subDynamicComponent:[%subcomponents%]:outFirstValue:value"
                    }
                    outSecondValue {
                        value "Application:composedComponent:subDynamicComponent:[%subcomponents%]:outSecondValue:value"
                    }
                }
            }
        }
    }
}