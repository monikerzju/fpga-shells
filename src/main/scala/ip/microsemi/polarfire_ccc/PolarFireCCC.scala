// See LICENSE for license details.
package sifive.fpgashells.ip.microsemi.polarfireccc

import Chisel._
import chisel3.experimental.{Analog,attach}
import freechips.rocketchip.util.{ElaborationArtefacts}
import freechips.rocketchip.util.GenericParameterizedBundle
import freechips.rocketchip.config._
import sifive.fpgashells.clocks._

/*
case class PolarFireCCCParameters(
  name:             String,
  pll_in_freq:      Double  = 50,
  gl0Enabled:       Boolean = false,
  gl1Enabled:       Boolean = false,
  gl2Enabled:       Boolean = false,
  gl3Enabled:       Boolean = false,
  gl0_0_out_freq:   Double  = 111.111,
  gl1_0_out_freq:   Double  = 111.111,
  gl2_0_out_freq:   Double  = 111.111,
  gl3_0_out_freq:   Double  = 111.111,
  gl0_0_pll_phase:  Double  = 0,
  gl1_0_pll_phase:  Double  = 0,
  gl2_0_pll_phase:  Double  = 0,
  gl3_0_pll_phase:  Double  = 0,
  feedback:         Boolean = false
)
*/

// Black Box for Microsemi PolarFire Clock Conditioning Circuit (CCC) Actel:SgCore:PF_CCC:1.0.112

class PolarFireCCCIOPads(c : PLLParameters) extends Bundle {
    val REF_CLK_0      = Clock(INPUT)
    val OUT0_FABCLK_0  = if (c.req.size >= 1) Some(Clock(OUTPUT)) else None
    val OUT1_FABCLK_0  = if (c.req.size >= 2) Some(Clock(OUTPUT)) else None
    val OUT2_FABCLK_0  = if (c.req.size >= 3) Some(Clock(OUTPUT)) else None
    val OUT3_FABCLK_0  = if (c.req.size >= 4) Some(Clock(OUTPUT)) else None
    val PLL_LOCK_0     = Bool(OUTPUT)
}

//scalastyle:off
//turn off linter: blackbox name must match verilog module
class PolarFireCCC(c : PLLParameters)(implicit val p:Parameters) extends BlackBox with PLL {
  val moduleName = c.name
  override def desiredName = c.name

  val io = new PolarFireCCCIOPads(c)
  
  def getLocked = io.PLL_LOCK_0
  def getClocks = Seq() ++ io.OUT0_FABCLK_0 ++ io.OUT1_FABCLK_0 ++ 
                           io.OUT2_FABCLK_0 ++ io.OUT3_FABCLK_0 
  
  def getClockNames = Seq.tabulate (c.req.size) { i =>
    s"${c.name}/inst/mmcm_adv_inst/CLKOUT${i}" 
  }
  
  var elaborateArtefactsString= "";
  var elaborateArtefactsString_temp= ""; //containg ctrl generate parameters
  
  elaborateArtefactsString_temp += s""" "PLL_IN_FREQ_0:${c.input.freqMHz}" \\
  |""".stripMargin
  for (i <- 0 until 4) {
      elaborateArtefactsString_temp += (if (i < c.req.size) 
          {s""" "GL${i.toString}_0_IS_USED:true" \\
          |""".stripMargin} 
          else 
          {s""" "GL${i.toString}_0_IS_USED:false" \\
          |""".stripMargin});
    }

  for (i <- 0 until c.req.size) {
      val freq = c.req(i).freqMHz.toString();
      val phase =  c.req(i).phaseDeg.toString();
      val jitter = c.req(i).jitter.toString();
      val phaseError =  c.req(i).phaseErrorDeg.toString();
      val dutyCycle =  c.req(i).dutyCycle.toString();
      
      elaborateArtefactsString_temp += 
        s""" "GL${i.toString}_0_OUT_FREQ:${freq}" \\
        | "GL${i.toString}_0_PLL_PHASE:${phase}" \\
        |""".stripMargin
    }

     elaborateArtefactsString_temp += s""" "PLL_FEEDBACK_MODE_0:${if (c.input.feedback) "External" else "Post-VCO"}" \\""" 

  elaborateArtefactsString += s""" create_design -id Actel:SgCore:PF_CCC:1.0.112 -design_name {${moduleName}} -config_file {} -params {} -inhibit_configurator 0
      | open_smartdesign -design {${moduleName}}
      | configure_design -component {${moduleName}} -library {} 
      | configure_vlnv_instance -component {${moduleName}} -library {} -name {${moduleName}_0} \\
      | -params {${elaborateArtefactsString_temp}
      | } -validate_rules 0 
      | fix_vlnv_instance -component {${moduleName}} -library {} -name {${moduleName}_0} 
      | open_smartdesign -design {${moduleName}} 
      | configure_design -component {${moduleName}} -library {}""".stripMargin

  
  ElaborationArtefacts.add(
    s"""AddIPInstance.${moduleName}.libero.tcl""",
    elaborateArtefactsString)
}
//scalastyle:on
