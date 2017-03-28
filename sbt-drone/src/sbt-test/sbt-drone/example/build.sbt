// This is obviously a trick so that `platformFetchPreviousArtifacts` succeed
organization := "me.vican.jorge"
name := "stoml"
scalaVersion in Global := "2.11.8"

licenses := Seq("MPL-2.0" -> url("http://opensource.org/licenses/MPL-2.0"))

lazy val checkVariablesContent =
  taskKey[Unit]("Check the content of variables")
checkVariablesContent := {
  if (insideDrone.value) {
    assert(droneEnvironment.value != None)
  }
}
