package com.drewmalin.vm.central.iaas.aws;

import com.drewmalin.vm.central.integrationtest.AbstractAwsTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.model.RuleState;
import software.amazon.awssdk.services.eventbridge.model.Target;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@Tag("integration")
public class AwsTest
    extends AbstractAwsTest {

    @Test
    void test() {

        final var aws = newAws();
        final var sqsQueueUrl = sqsQueueUrl(aws);

        final var response = aws.ec2(Region.US_WEST_2)
            .runInstances(builder -> {
                builder.imageId("ami-ff0fea8310f3");
                builder.maxCount(1);
                builder.instanceType("t3.nano");
            });

        assertThat(response.instances().size(), is(1));

        final var instance = response.instances().get(0);

//        assertThat(instance.state(), is(InstanceState.builder()
//            .code(0)
//            .name(InstanceStateName.PENDING)
//            .build()));

        aws.ec2(Region.US_WEST_2)
            .terminateInstances(builder -> {
                builder.instanceIds(instance.instanceId());
            });

//        assertThat(instance.state(), is(InstanceState.builder()
//            .code(0)
//            .name(InstanceStateName.SHUTTING_DOWN)
//            .build()));

//        aws.sqs(Region.US_WEST_2).sendMessage(builder -> {
//            builder.queueUrl(sqsQueueUrl);
//            builder.messageBody("hi");
//        });

        final var messages = aws.sqs(Region.US_WEST_2)
            .receiveMessage(builder -> {
                builder.queueUrl(sqsQueueUrl);
            })
            .messages();

        assertThat(messages.size(), is(0));
    }

    private String sqsQueueUrl(final Aws aws) {
        final var queueUrl = aws.sqs(Region.US_WEST_2)
            .createQueue(builder -> {
                builder.queueName("ec2-things-queue");
            })
            .queueUrl();

        final var queueArn = aws.sqs(Region.US_WEST_2)
            .getQueueAttributes(builder -> {
                builder.queueUrl(queueUrl);
                builder.attributeNames(QueueAttributeName.QUEUE_ARN);
            })
            .attributes()
            .get(QueueAttributeName.QUEUE_ARN);

        final var ruleName = "ec2-things-rule";
        final var ruleArn = aws.eventBridge(Region.US_WEST_2)
            .putRule(builder -> {
                builder.name(ruleName);
                builder.eventBusName("default");
                builder.eventPattern("""
                    {
                      "source": ["aws.ec2"]
                    }
                    """);
                builder.state(RuleState.ENABLED);
            }).ruleArn();

        aws.sqs(Region.US_WEST_2)
            .setQueueAttributes(builder -> {
                builder.queueUrl(queueUrl);

                final var attributes = new HashMap<QueueAttributeName, String>();
                attributes.put(QueueAttributeName.POLICY, """
                    {
                      "Version": "2012-10-17",
                      "Statement":[{
                        "Effect": "Allow",
                        "Principal": {
                          "AWS": "*"
                        },
                        "Action": "sqs:SendMessage",
                        "Resource": "%s",
                        "Condition": {
                          "ArnEquals": {
                            "aws:SourceArn": "%s"
                          }
                        }
                      }]"
                    }
                    """.formatted(queueArn, ruleArn));
                builder.attributes(attributes);
            });

        final var putTargetsResponse = aws.eventBridge(Region.US_WEST_2)
            .putTargets(builder -> {
                builder.rule(ruleName);
                builder.targets(
                    Target.builder()
                        .id("ec2-things-queue")
                        .arn(queueArn)
                        .inputPath("$.detail")
                        .build()
                );
            });

        return queueUrl;
    }
}
