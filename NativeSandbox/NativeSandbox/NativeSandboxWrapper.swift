//
//  NativeSandboxWrapper.swift
//  NativeSandbox
//
//  Created by Iurii Malchenko on 13.05.2025.
//

import Foundation
import NativeSandboxLib

enum NativeSandboxWrapper {
    private static var initialized = false

    static func Init() {
        guard !initialized else { return }
        let result = ScalaNativeInit()
        if result != 0 {
            fatalError("Failed to initialize the sn runtime. Cannot continue.")
        }
        initialized = true
    }

    static func native_sandbox_test(
        _ input: String,
    ) -> String {
        return interop(
            NativeSandboxLib.native_sandbox_test,
            input: input
        )
    }

    ///  helpers

    private static var busy = false

    private static func interop(
        _ native: (UnsafePointer<CChar>) -> UnsafePointer<CChar>?,
        input: String
    ) -> String {
        if !initialized {
            print("!!--------- sn runtime not initialized ---------!!")
            return """
            { "error": "sn runtime not initialized" }
            """
        }
        if busy {
            print("!!--------- concurrent invocation ---------!!")
            return """
            { "error": "concurrent invocations are not allowed" }
            """
        }
        busy = true
        // Convert Swift string to C string
        guard let cString = input.cString(using: .utf8) else {
            return "Error: could not convert input to C string"
        }
        // Call the C function
        let resultCString = native(cString)

        // Handle possible null result
        guard let resultPtr = resultCString else {
            return "Error: scala_native returned null"
        }

        // Convert the result back to a Swift string
        guard let result = String(cString: resultPtr, encoding: .utf8) else {
            // Free the memory even if conversion fails
            free_bridge_result(resultPtr)
            return "Error: could not convert result to Swift string"
        }

        // Free the memory allocated by the library
        free_bridge_result(resultPtr)

        busy = false
        return result
    }
    
}
