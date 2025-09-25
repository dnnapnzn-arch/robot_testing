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
    private static final double CLAW_OPEN   = 0.7;
    private static final double CLAW_CLOSED = 0.35;

    private static final double DRIVE_MAX = 0.40;
    private static final double TURN_MAX  = 0.60;
    private static final double SLOW_MULT = 0.50;
    private static final boolean INVERT_DRIVE = true;

    private static final double ARM_UP_MAX   = 0.35;
    private static final double ARM_DOWN_MAX = 0.20;

    private static final double DRIVE_LEFT_GAIN  = 1.30;
    private static final double DRIVE_RIGHT_GAIN = 1.00;

    @Override
    public void runOpMode() {
        leftDrive = hardwareMap.get(DcMotor.class, "leftDrive");
        rightDrive = hardwareMap.get(DcMotor.class, "rightDrive");
        arm       = hardwareMap.get(DcMotor.class, "arm");
        claw      = hardwareMap.get(Servo.class,   "claw");
        armLimit  = hardwareMap.get(TouchSensor.class, "armLimit");

        leftDrive.setDirection(DcMotor.Direction.REVERSE);
        rightDrive.setDirection(DcMotor.Direction.FORWARD);
        leftDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        arm.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        arm.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        claw.setPosition(CLAW_CLOSED);

        telemetry.addData("Status", "Initialized");
        telemetry.update();

        waitForStart();
        if (isStopRequested()) return;

        boolean clawOpen = false;

        while (opModeIsActive()) {
            // ----- drive: arcade -----
            double driveInput = -gamepad1.left_stick_y;
            if (INVERT_DRIVE) driveInput = -driveInput;
            double turnInput  =  gamepad1.right_stick_x;

            double drive = Range.clip(driveInput * DRIVE_MAX, -1.0, 1.0);
            double turn  = Range.clip(turnInput  * TURN_MAX,  -1.0, 1.0);

            double leftPower  = Range.clip(drive + turn, -1.0, 1.0);
            double rightPower = Range.clip(drive - turn, -1.0, 1.0);

            if (gamepad1.right_bumper) {
                leftPower  *= SLOW_MULT;
                rightPower *= SLOW_MULT;
            }

            leftPower  = Range.clip(leftPower  * DRIVE_LEFT_GAIN,  -1.0, 1.0);
            rightPower = Range.clip(rightPower * DRIVE_RIGHT_GAIN, -1.0, 1.0);

            leftDrive.setPower(leftPower);
            rightDrive.setPower(rightPower);

            // ----- arm control with limit switch block -----
            double up   = gamepad1.right_trigger;
            double down = gamepad1.left_trigger;
            boolean limitPressed = armLimit.isPressed();

            double armPower = up * ARM_UP_MAX - down * ARM_DOWN_MAX;
            if (armPower < 0 && limitPressed) armPower = 0;
            if (gamepad1.left_bumper) armPower *= 0.5;
            arm.setPower(armPower);

            // ----- claw control -----
            if (gamepad1.a)      clawOpen = true;
            else if (gamepad1.b) clawOpen = false;
            claw.setPosition(clawOpen ? CLAW_OPEN : CLAW_CLOSED);

            // ----- telemetry -----
            telemetry.addData("Drive", "L %.2f  R %.2f", leftPower, rightPower);
            telemetry.addData("Gains", "L %.2f  R %.2f", DRIVE_LEFT_GAIN, DRIVE_RIGHT_GAIN);
            telemetry.addData("Arm", "%.2f  (limit: %s)", armPower, limitPressed ? "PRESSED" : "open");
            telemetry.update();
        }
    }
}
