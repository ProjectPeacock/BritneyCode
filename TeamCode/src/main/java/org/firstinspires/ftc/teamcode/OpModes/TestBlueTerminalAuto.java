package org.firstinspires.ftc.teamcode.OpModes;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;
import org.firstinspires.ftc.teamcode.Hardware.HWProfile;
import org.firstinspires.ftc.teamcode.Libs.LiftControlClass;
import org.firstinspires.ftc.teamcode.Libs.AutoParams;
import org.firstinspires.ftc.teamcode.drive.SampleMecanumDrive;
import org.firstinspires.ftc.teamcode.trajectorysequence.TrajectorySequence;

import java.util.List;

@Autonomous(name = " Test Auto: Blue Terminal", group = "Competition")

public class TestBlueTerminalAuto extends LinearOpMode {
    /*


    OPMODE MAP - PLEASE READ BEFORE EDITING

    This opMode uses TrajectorySequences from RoadRunner. They are made to be run back to back.
    The order of operations is: untilCycle -> firstCycle -> cycles2to4 (repeated 3 times) -> highCycle -> park

    Each parking position is its own TrajectorySequence. They are all made to be run following highCycle.

    All values for target position and heading come from AutoParams.java.

     */

    public static double preloadX = 28.5;
    public static double preloadY = -31;
    //lift control init
    public final static HWProfile robot = new HWProfile();
    private LinearOpMode myOpmode=this;

    //init params
    AutoParams params = new AutoParams();

    //TFOD init
    private static final String TFOD_MODEL_ASSET = robot.tfliteFileName;
    private static final String[] LABELS = {
            "circle",
            "triangle",
            "star"
    };
    private static final String VUFORIA_KEY =
            "AfHl2GP/////AAABmeJc93xOhk1MvZeKbP5E43taYJ6kodzkhsk5wOLGwZI3wxf7v1iTx2Mem/VZSEtpxb3U2fMO7n0EUxSeHRWhOXeX16dMFcjfalezjo3ZkzBuG/y2r4kgLwKs4APyAIClBAon+tf/W/4NkTkYuHGo8zZ0slH/iBpqxvblpNURsG5h4VxPFgF5D/FIfmjnddzQpa4cGarle/Zvuah6q2orUswun31P6ZLuIJvdOIQf7o/ruoRygsSXfVYc35w+Xwm+bwjpZUNzHHYvRNrp0HNWC3Fr2hd0TqWKIIYlCoHj0m5OKX22Ris23V8PdKM/i4/ZIy8JewJXetv1rERC5bfHmUXCS4Rl7RjR+ZscQ5aA0nr8";
    private VuforiaLocalizer vuforia;
    private TFObjectDetector tfod;
    int parkPos = 2;

    @Override
    public void runOpMode() {
        initVuforia();
        initTfod();
        DcMotorEx motorLiftF = null;
        DcMotorEx motorLiftR = null;
        robot.init(hardwareMap);


        // start up the shooter control program which runs in parallel
        LiftControlClass liftControl = new LiftControlClass(robot, myOpmode);
        if (tfod != null) {
            tfod.activate();

            // The TensorFlow software will scale the input images from the camera to a lower resolution.
            // This can result in lower detection accuracy at longer distances (> 55cm or 22").
            // If your target is at distance greater than 50 cm (20") you can increase the magnification value
            // to artificially zoom in to the center of image.  For best results, the "aspectRatio" argument
            // should be set to the value of the images used to create the TensorFlow Object Detection model
            // (typically 16/9).
            tfod.setZoom(1.0, 16.0/9.0);
        }

        SampleMecanumDrive drive = new SampleMecanumDrive(hardwareMap);


        Pose2d startPose= new Pose2d(params.startPoseX,params.startPoseY,Math.toRadians(90));
        drive.setPoseEstimate(startPose);

        TrajectorySequence untilCycle = drive.trajectorySequenceBuilder(startPose)
                .UNSTABLE_addTemporalMarkerOffset(0, liftControl::closeClaw)
                .waitSeconds(0.5)
                .UNSTABLE_addTemporalMarkerOffset(0.25,()->{liftControl.moveLiftScore(2);})
                .splineTo(new Vector2d(preloadX,preloadY),Math.toRadians(120))

                .UNSTABLE_addTemporalMarkerOffset(0.35, liftControl::openClaw)
                .waitSeconds(0.35)
                .back(3)
                .UNSTABLE_addTemporalMarkerOffset(-0.125,()->{liftControl.moveLiftGrab();})
                .turn(Math.toRadians(-45))
                .splineToLinearHeading(new Pose2d(61,-10,Math.toRadians(0)),Math.toRadians(0))

                .UNSTABLE_addTemporalMarkerOffset(0, liftControl::closeClaw)
                .waitSeconds(0.35)
                .build();

        TrajectorySequence cycleMid = drive.trajectorySequenceBuilder(untilCycle.end())
                .UNSTABLE_addTemporalMarkerOffset(0,()->{liftControl.moveLiftScore(2);})
                .back(6)
                .splineToSplineHeading(new Pose2d(31.5,-18.5,Math.toRadians(220)),Math.toRadians(190))
                .waitSeconds(0.25)
                .UNSTABLE_addTemporalMarkerOffset(-0.25, liftControl::openClaw)
                .back(1)
                .UNSTABLE_addTemporalMarkerOffset(0.25, liftControl::moveLiftGrab)
                .splineToSplineHeading(new Pose2d(61,-10,Math.toRadians(0)),Math.toRadians(0))
                .UNSTABLE_addTemporalMarkerOffset(0,liftControl::closeClaw)
                .waitSeconds(0.5)
                .build();

        TrajectorySequence cycleHigh = drive.trajectorySequenceBuilder(cycleMid.end())
                .UNSTABLE_addTemporalMarkerOffset(0,()->{liftControl.moveLiftScore(3);})
                .back(6)
                .splineToSplineHeading(new Pose2d(30,-6.5,Math.toRadians(130)),Math.toRadians(220))
                .waitSeconds(0.25)
                .UNSTABLE_addTemporalMarkerOffset(-0.25, liftControl::openClaw)
                .back(1)
                .UNSTABLE_addTemporalMarkerOffset(0.25, liftControl::moveLiftGrab)
                .splineToSplineHeading(new Pose2d(60,-12,Math.toRadians(0)),Math.toRadians(0))
                .UNSTABLE_addTemporalMarkerOffset(0,liftControl::closeClaw)
                .waitSeconds(0.5)
                .build();

        TrajectorySequence finalMid = drive.trajectorySequenceBuilder(cycleMid.end())
                .UNSTABLE_addTemporalMarkerOffset(0,()->{liftControl.moveLiftScore(2);})
                .back(6)
                .splineToSplineHeading(new Pose2d(31.5,-18.5,Math.toRadians(220)),Math.toRadians(190))
                .waitSeconds(0.25)
                .UNSTABLE_addTemporalMarkerOffset(-0.25,()->{liftControl.openClaw();})
                .back(1)
                .build();

        TrajectorySequence finalHigh = drive.trajectorySequenceBuilder(cycleHigh.end())
                .UNSTABLE_addTemporalMarkerOffset(0,()->{liftControl.moveLiftScore(3);})
                .back(6)
                .splineToSplineHeading(new Pose2d(30,-6.5,Math.toRadians(130)),Math.toRadians(220))
                .waitSeconds(0.25)
                .UNSTABLE_addTemporalMarkerOffset(-0.25,()->{liftControl.openClaw();})
                .back(1)
                .build();

        TrajectorySequence park1 = drive.trajectorySequenceBuilder(finalHigh.end())
                .UNSTABLE_addTemporalMarkerOffset(0,()->{liftControl.moveLiftScore(0);})
                .splineToSplineHeading(new Pose2d(12,-14,Math.toRadians(90)),Math.toRadians(180))
                .build();

        TrajectorySequence park2 = drive.trajectorySequenceBuilder(finalHigh.end())
                .UNSTABLE_addTemporalMarkerOffset(0,()->{liftControl.moveLiftScore(0);})
                .splineToSplineHeading(new Pose2d(36,-14,Math.toRadians(90)),Math.toRadians(130))
                .build();

        TrajectorySequence park3 = drive.trajectorySequenceBuilder(finalHigh.end())
                .UNSTABLE_addTemporalMarkerOffset(0,()->{liftControl.moveLiftScore(0);})
                .splineToSplineHeading(new Pose2d(60,-14,Math.toRadians(90)),Math.toRadians(0))
                .build();

        while(!isStarted() && !isStopRequested()) {
            if (tfod != null) {
                robot.autoLight.set(-1);
                // getUpdatedRecognitions() will return null if no new information is available since
                // the last time that call was made.
                List<Recognition> updatedRecognitions = tfod.getUpdatedRecognitions();
                if (updatedRecognitions != null) {
                    telemetry.addData("# Objects Detected", updatedRecognitions.size());

                    // step through the list of recognitions and display image position/size information for each one
                    // Note: "Image number" refers to the randomized image orientation/number
                    for (Recognition recognition : updatedRecognitions) {
                        double col = (recognition.getLeft() + recognition.getRight()) / 2 ;
                        double row = (recognition.getTop()  + recognition.getBottom()) / 2 ;
                        double width  = Math.abs(recognition.getRight() - recognition.getLeft()) ;
                        double height = Math.abs(recognition.getTop()  - recognition.getBottom()) ;

                        telemetry.addData(""," ");
                        telemetry.addData("Image", "%s (%.0f %% Conf.)", recognition.getLabel(), recognition.getConfidence() * 100 );
                        telemetry.addData("- Position (Row/Col)","%.0f / %.0f", row, col);
                        telemetry.addData("- Size (Width/Height)","%.0f / %.0f", width, height);

                        if(recognition.getLabel() == "circle"){
                            parkPos = 1;
                        } else if(recognition.getLabel() == "star" ){
                            parkPos = 3;
                        } else parkPos = 2;
                    }
                    telemetry.update();
                }
            }

        }  // end of while

        robot.autoLight.set(0);
        if(isStopRequested()) return;

        //score preload
        drive.followTrajectorySequence(untilCycle);
        for(int i=0;i<params.numMidCycles;i++){
            drive.followTrajectorySequence(cycleMid);
        }

        /*
        for(int i=0;i<params.numHighCycles;i++){
            drive.followTrajectorySequence(cycleHigh);
        }
        */

        drive.followTrajectorySequence(finalMid);

        if(parkPos==1){
            drive.followTrajectorySequence(park1);
        }else if(parkPos==2){
            drive.followTrajectorySequence(park2);
        }else{
            drive.followTrajectorySequence(park3);
        }

    }
    private void initVuforia() {
        /*
         * Configure Vuforia by creating a Parameter object, and passing it to the Vuforia engine.
         */
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();

        parameters.vuforiaLicenseKey = VUFORIA_KEY;
        parameters.cameraName = hardwareMap.get(WebcamName.class, "Webcam 1");

        //  Instantiate the Vuforia engine
        vuforia = ClassFactory.getInstance().createVuforia(parameters);
    }


    /**
     * Initialize the TensorFlow Object Detection engine.
     */
    private void initTfod() {
        int tfodMonitorViewId = hardwareMap.appContext.getResources().getIdentifier(
                "tfodMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        TFObjectDetector.Parameters tfodParameters = new TFObjectDetector.Parameters(tfodMonitorViewId);
        tfodParameters.minResultConfidence = 0.6f;
        tfodParameters.isModelTensorFlow2 = true;
        tfodParameters.inputSize = 300;

        tfod = ClassFactory.getInstance().createTFObjectDetector(tfodParameters, vuforia);

        // Use loadModelFromAsset() if the TF Model is built in as an asset by Android Studio
        // Use loadModelFromFile() if you have downloaded a custom team model to the Robot Controller's FLASH.
        tfod.loadModelFromAsset(TFOD_MODEL_ASSET, LABELS);
        // tfod.loadModelFromFile(TFOD_MODEL_FILE, LABELS);
    }
}
