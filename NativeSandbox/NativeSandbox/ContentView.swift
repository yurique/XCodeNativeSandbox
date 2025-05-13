//
//  ContentView.swift
//  NativeSandbox
//
//  Created by Iurii Malchenko on 13.05.2025.
//

import SwiftUI

struct ContentView: View {
    
    init() {
        NativeSandboxWrapper.Init()
    }
    
    private func systemTimezoneId() -> String {
        TimeZone.current.identifier
    }
    
    var body: some View {
        VStack {
            Image(systemName: "globe")
                .imageScale(.large)
                .foregroundStyle(.tint)
            Text("Hello, world!")
            Button(
                "Invoke the other universe!"
            ) {
                let input =
                    """
                    {
                        "data": {
                            "testKey": "test value"
                        },
                        "depth": 10,
                        "systemTimezone": "\(systemTimezoneId())"
                    }
                    """
                let result = NativeSandboxWrapper.native_sandbox_test(input)
                
                print(result.count)
                if result.contains("snError") || result.contains("error") {
                    print(result)
                } else {
                    let start = result.startIndex
                    let end = result.index(result.startIndex, offsetBy: min(50, result.count))
                    print(result[start..<end] + " ...")
                }
//                NativeSandboxWrapper.native_sandbox_test_no_input()
            }
        }
        .padding()
    }
}

#Preview {
    ContentView()
}
