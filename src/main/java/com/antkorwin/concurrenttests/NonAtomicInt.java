package com.antkorwin.concurrenttests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Created by Korovin Anatolii on 05.07.2018.
 *
 * @author Korovin Anatolii
 * @version 1.0
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
class NonAtomicInt {
    private int value;

    public void increment() {
        ++value;
    }
}
