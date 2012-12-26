/* (C) 2012 Pragmatic Software
   This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/
 */

package com.googlecode.networklog;

public class ItemNotFoundException extends Exception {
  public ItemNotFoundException() {
    super();
  }

  public ItemNotFoundException(String message) {
    super(message);
  }

  public ItemNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public ItemNotFoundException(Throwable cause) {
    super(cause);
  }
}
