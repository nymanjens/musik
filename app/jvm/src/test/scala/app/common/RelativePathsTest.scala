package app.common

import app.common.testing._
import org.junit.runner._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class RelativePathsTest extends HookedSpecification {

  "joinPaths" in {
    RelativePaths.joinPaths("abc/", "def") mustEqual "abc/def"
    RelativePaths.joinPaths("abc", "def") mustEqual "abc/def"
    RelativePaths.joinPaths("abc/d", "efg") mustEqual "abc/d/efg"
  }

  "getFilename" in {
    RelativePaths.getFilename("abc/defg.h") mustEqual "defg.h"
  }

  "getFolderPath" in {
    RelativePaths.getFolderPath("/abc/def/ghi/jkl.mn") mustEqual "/abc/def/ghi"
  }
}
