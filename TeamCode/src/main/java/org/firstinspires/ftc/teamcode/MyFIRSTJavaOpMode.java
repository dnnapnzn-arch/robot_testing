package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.TouchSensor;
import com.qualcomm.robotcore.util.Range;

@TeleOp(name = "MyFIRSTJavaOpMode")
public class MyFIRSTJavaOpMode extends LinearOpMode {
    private DcMotor leftDrive, rightDrive, arm;
    private Servo claw;
    private TouchSensor armLimit;

    // ---- Tuning constants ----
    private static final double CLAW_OPEN   = 0.7;   // servo positions (0..1)
    private static final double CLAW_CLOSED = 0.35;

    // Drive caps (reduce if the bot is too fast)
    private static final double DRIVE_MAX = 0.25;   // forward/back scale (0..1)
    private static final double TURN_MAX  = 0.25;   // turning scale (0..1)
    private static final double SLOW_MULT = 0.50;   // hold right bumper for "slow mode"

    // Flip stick direction without changing motor wiring
    // If pushing stick up makes the robot go backward, set this to true.
    private static final boolean INVERT_DRIVE = true;

    // Arm power caps
    private static final double ARM_UP_MAX   = 0.20;
    private static final double ARM_DOWN_MAX = 0.20;

    @Override
    public void runOpMode() {
        // ----- hardware map (names must match your RC configuration) -----
        leftDrive = hardwareMap.get(DcMotor.class, "leftDrive");
        rightDrive = hardwareMap.get(DcMotor.class, "rightDrive");
        arm       = hardwareMap.get(DcMotor.class, "arm");
        claw      = hardwareMap.get(Servo.class,   "claw");
        armLimit  = hardwareMap.get(TouchSensor.class, "armLimit");

        // ----- motor/servo setup -----
        // Keep these if the wheels spin opposite directions when given the same power.
        leftDrive.setDirection(DcMotor.Direction.REVERSE);
        rightDrive.setDirection(DcMotor.Direction.FORWARD);

        leftDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        arm.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        arm.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        claw.setPosition(CLAW_CLOSED); // start closed

        telemetry.addData("Status", "Initialized");
        telemetry.update();

        waitForStart();
        if (isStopRequested()) return;

        boolean clawOpen = false;

        while (opModeIsActive()) {
            // ----- drive: arcade -----
            double driveInput = -gamepad1.left_stick_y;   // up on stick is negative → invert
            if (INVERT_DRIVE) driveInput = -driveInput;   // flip forward/back if needed
            double turnInput  =  gamepad1.right_stick_x;

            // apply caps
            double drive = Range.clip(driveInput * DRIVE_MAX, -1.0, 1.0);
            double turn  = Range.clip(turnInput  * TURN_MAX,  -1.0, 1.0);

            double leftPower  = Range.clip(drive + turn, -1.0, 1.0);
            double rightPower = Range.clip(drive - turn, -1.0, 1.0);

            // slow mode with right bumper
            if (gamepad1.right_bumper) {
                leftPower  *= SLOW_MULT;
                rightPower *= SLOW_MULT;
            }

            leftDrive.setPower(leftPower);
            rightDrive.setPower(rightPower);

            // ----- arm control with limit switch block -----
            double up   = gamepad1.right_trigger;  // 0..1
            double down = gamepad1.left_trigger;   // 0..1
            boolean limitPressed = armLimit.isPressed(); // true when pressed

            double armPower = up * ARM_UP_MAX - down * ARM_DOWN_MAX;
            if (armPower < 0 && limitPressed) armPower = 0; // block downward motion at limit
            if (gamepad1.left_bumper) armPower *= 0.5;      // precision mode
            arm.setPower(armPower);

            // ----- claw control -----
            if (gamepad1.a)      clawOpen = true;
            else if (gamepad1.b) clawOpen = false;
            claw.setPosition(clawOpen ? CLAW_OPEN : CLAW_CLOSED);

            // ----- telemetry -----
            telemetry.addData("Drive", "L %.2f  R %.2f", leftPower, rightPower);
            telemetry.addData("Arm", "%.2f  (limit: %s)", armPower, limitPressed ? "PRESSED" : "open");
            telemetry.addData("Claw", clawOpen ? "OPEN" : "CLOSED");
            telemetry.update();
        }
    }
}
